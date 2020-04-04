/*
 * Copyright 2020 Pierre Nodet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package aruku

import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.{ HashPartitioner, Partitioner }
import org.apache.spark.broadcast.Broadcast
import scala.collection.immutable.HashMap
import aruku.util._
import aruku.sampling._
import aruku.partition._

sealed case class WalkerState[T, M] private[aruku] (
  walker: Walker[T],
  path: Array[VertexId],
  msg: Option[M],
  done: Boolean
)

sealed case class WalkEngine[T, M] private[aruku] (partitioner: Partitioner) {

  private def initRoutingTable(
    graph: RDD[(VertexId, Array[Edge[Double]])],
    transitionBC: Broadcast[Transition[T, M]]
  ): RDD[PartitionID] =
    graph.mapPartitionsWithIndex(
      { (pid: PartitionID, iter: Iterator[(VertexId, Array[Edge[Double]])]) =>
        val static = transitionBC.value.static
        LocalGraphPartition.data ++= iter.map {
          case (vid, data) => {
            val components    = data.map(edge => static(vid, edge))
            val sum           = components.sum
            val probabilities = components.map(_ / sum)
            val aliases       = AliasSampling.fromRawProbabilities(probabilities)
            (vid, LocalData(data, aliases))
          }
        }
        Iterator.empty
      },
      preservesPartitioning = true
    )

  private def initWalkers(
    vertices: RDD[VertexId],
    walkerConfigBC: Broadcast[WalkerConfig[T]]
  ): RDD[(VertexId, WalkerState[T, M])] = {
    val walkerConfigGlobal = walkerConfigBC.value
    val walkers: RDD[(VertexId, Walker[T])] = vertices.mapPartitions(
      { iter =>
        val walkerConfigLocal = walkerConfigBC.value
        iter.flatMap { vid =>
          walkerConfigLocal.start match {
            case AtRandom(probability, random) =>
              if (random.nextDouble() < probability) Some(vid, Walker[T](0, 1, walkerConfigLocal.init(vid)))
              else None
            case FromVertices(vertices) =>
              if (vertices.contains(vid)) Some(vid, Walker[T](0, 1, walkerConfigLocal.init(vid))) else None
          }
        }
      },
      preservesPartitioning = true
    )

    val actualWalkers: Long = walkers.count()

    val fullWalkers =
      if (actualWalkers < walkerConfigGlobal.numWalkers) {
        var i   = 0
        var acc = walkers
        while (i * actualWalkers < walkerConfigGlobal.numWalkers - actualWalkers) {
          acc = acc.union(walkers)
          i += 1
        }
        acc
      } else {
        walkers
      }

    fullWalkers
      .zipWithIndex()
      .mapPartitions({
        _.map {
          case ((vid, walker), id) =>
            (vid, WalkerState[T, M](walker.copy(id = id), Array.empty[VertexId], Option.empty[M], false))
        }
      }, preservesPartitioning = true)
      .filter(_._2.walker.id < walkerConfigBC.value.numWalkers)
  }

  private def transferWalkers(
    routingTable: RDD[PartitionID],
    walkers: RDD[(VertexId, WalkerState[T, M])]
  ): RDD[(VertexId, WalkerState[T, M])] =
    routingTable.zipPartitions {
      walkers.partitionBy(partitioner)
    } { (_, walker) =>
      walker
    }

  private def walk(
    walkers: RDD[(VertexId, WalkerState[T, M])],
    walkerConfigBC: Broadcast[WalkerConfig[T]],
    transitionBC: Broadcast[Transition[T, M]]
  ): RDD[(VertexId, WalkerState[T, M])] =
    walkers.mapPartitions(
      { iter =>
        val update     = walkerConfigBC.value.update
        val transition = transitionBC.value
        val extension  = transition.extension
        val msg        = transition.msg

        iter.map {
          case (vid, state) =>
            var updateVid   = vid
            var updateState = state
            var doneLocally = false

            while (!updateState.done && !doneLocally) {
              val localData = LocalGraphPartition.data.get(updateVid)
              localData match {
                case None =>
                  updateState = updateState.copy(msg = None, done = true)
                case Some(LocalData(neighbors, alias)) =>
                  val rejection = RejectionSampling.fromWalkerTransition[T, M](updateState.walker, transition)
                  val nextEdge  = neighbors(rejection.next(updateVid, neighbors, updateState.msg, alias))
                  val nvid      = nextEdge.dstId
                  val isDone    = !(alias.random.nextDouble() < extension(updateState.walker, updateVid))
                  doneLocally = !LocalGraphPartition.data.contains(nvid)
                  updateState = WalkerState(
                    updateState.walker
                      .copy(
                        step = updateState.walker.step + 1,
                        data = update(updateState.walker, updateVid, nextEdge)
                      ),
                    updateState.path ++ Array(updateVid),
                    msg(updateState.walker, updateVid, neighbors),
                    isDone
                  )
                  updateVid = nvid
              }
            }
            (updateVid, updateState)
        }
      },
      preservesPartitioning = false
    )

  def randomWalk(
    graph: RDD[(VertexId, Array[Edge[Double]])],
    walkerConfig: WalkerConfig[T],
    transition: Transition[T, M]
  ): RDD[(Long, Array[VertexId])] = {

    val sc = graph.sparkContext

    val repartitioned = graph.partitionBy(partitioner).cache()

    val transitionBC   = sc.broadcast(transition)
    val walkerConfigBC = sc.broadcast(walkerConfig)

    var walkers = initWalkers(repartitioned.keys, walkerConfigBC).cache()
    val checkpointInterval = sc.getConf
      .getInt("spark.graphx.pregel.checkpointInterval", -1)
    val walkerCheckpointer = new RDDCheckpointer[(VertexId, WalkerState[T, M])](checkpointInterval, sc)
    walkerCheckpointer.update(walkers)
    var numWalkingWalkers = walkers.filter {
      case (_, state) => !state.done
    }.count()

    var completeWalkers = HashMap(walkers.map {
      case (_, state) => (state.walker.id, Array.empty[VertexId])
    }.collect(): _*)

    val routingTable = initRoutingTable(repartitioned, transitionBC).cache()
    routingTable.count()

    repartitioned.unpersist()

    var prevWalkers: RDD[(VertexId, WalkerState[T, M])] = null

    while (numWalkingWalkers > 0) {

      prevWalkers = walkers

      walkers = walk(transferWalkers(routingTable, walkers), walkerConfigBC, transitionBC)
      walkerCheckpointer.update(walkers)

      completeWalkers = completeWalkers.merged(HashMap(walkers.map {
        case (_, state) => (state.walker.id, state.path)
      }.collect(): _*)) { case ((k1, v1), (k2, v2)) => (k1, v1 ++ v2) }

      walkers = walkers.filter {
        case (_, state) => !state.done
      }.mapValues { state =>
        state.copy(path = Array.empty)
      }
      numWalkingWalkers = walkers.count()

      prevWalkers.unpersist()

    }

    //Cleaning Data
    walkerCheckpointer.deleteAllCheckpoints()
    routingTable.foreachPartition { iter =>
      LocalGraphPartition.data.clear()
    }
    routingTable.unpersist()

    sc.parallelize(completeWalkers.toSeq)

  }

}

object WalkEngine {

  def fromNumPartitions[T, M](numPartions: Int) =
    new WalkEngine[T, M](new HashPartitioner(numPartions))

  def fromPartitioner[T, M](partitioner: Partitioner) =
    new WalkEngine[T, M](partitioner)

}
