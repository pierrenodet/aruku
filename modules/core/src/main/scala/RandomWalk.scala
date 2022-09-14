/*
 * Copyright 2019 Pierre Nodet
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

import aruku.partition._
import aruku.sampling._
import aruku.util._
import org.apache.spark.HashPartitioner
import org.apache.spark.Partitioner
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

import java.util.concurrent.Executors
import scala.collection.immutable.ArraySeq
import scala.concurrent._
import scala.reflect.ClassTag
import scala.util.Random

import duration._

final case class WalkerState[T, M] private[aruku] (
  walker: Walker[T],
  path: Array[VertexId],
  message: Option[M],
  done: Boolean
)

object RandomWalk {

  private def initRoutingTable[T, M](
    graph: RDD[(VertexId, Array[Edge[Double]])],
    transitionBC: Broadcast[Transition[T, M]],
    partitioner: Partitioner
  ): RDD[Int] =
    graph
      .partitionBy(partitioner)
      .mapPartitionsWithIndex(
        { (_, iter) =>
          val static = transitionBC.value.static
          LocalGraphPartition.data ++= iter.map { case (vid, data) =>
            val probabilities = data.map(edge => static(vid, edge))
            val aliases       = AliasMethod.fromRawProbabilities(probabilities)
            (vid, LocalData(data, aliases))
          }
          Iterator.empty
        },
        preservesPartitioning = true
      )

  private def initWalkers[T, M](
    vertices: RDD[VertexId],
    walkerConfigBC: Broadcast[WalkerConfig[T]],
    partitioner: Partitioner
  ): Array[RDD[(VertexId, WalkerState[T, M])]] = {

    val sc = vertices.sparkContext

    val walkerConfigGlobal = walkerConfigBC.value

    val numWalkers = walkerConfigGlobal.numWalkers
    val numEpochs  = walkerConfigGlobal.numEpochs

    val walkers: RDD[(VertexId, Walker[T])] = vertices
      .mapPartitions(
        { iter =>
          val walkerConfigLocal = walkerConfigBC.value
          iter.flatMap { vid =>
            walkerConfigLocal.start match {
              case AtRandom(probability)  =>
                if (Random.nextDouble() < probability) Some(vid, Walker[T](0, 0, walkerConfigLocal.init(vid)))
                else None
              case FromVertices(vertices) =>
                if (vertices.contains(vid)) Some(vid, Walker[T](0, 0, walkerConfigLocal.init(vid))) else None
            }
          }
        },
        preservesPartitioning = true
      )
      .partitionBy(partitioner)
      .cache()

    val actualWalkers: Long = walkers.count()

    val tooMuchWalkers =
      if (actualWalkers < numWalkers) {
        var i   = 0
        var acc = Array(walkers)
        while (i * actualWalkers < numWalkers - actualWalkers) {
          acc = acc ++ Array(walkers)
          i += 1
        }
        sc.union(acc.toSeq)
      } else {
        walkers
      }

    val fullWalkersReady = tooMuchWalkers
      .zipWithIndex()
      .mapPartitions(
        _.map { case ((vid, walker), id) =>
          (vid, WalkerState[T, M](walker.copy(id = id), Array.empty[VertexId], Option.empty[M], false))
        },
        preservesPartitioning = true
      )
      .cache()

    val batchedWalkers = {
      for (i <- 0 until numEpochs) yield fullWalkersReady.filter { case (_, state) =>
        val numWalkers = walkerConfigBC.value.numWalkers
        val numEpochs  = walkerConfigBC.value.numEpochs
        (state.walker.id < ((i + 1) * numWalkers / numEpochs)) && (state.walker.id >= (i * numWalkers / numEpochs))
      }
    }.toArray
    // Materialized Walkers
    batchedWalkers.map(_.count())

    fullWalkersReady.unpersist()
    walkers.unpersist()

    batchedWalkers
  }

  private def transferWalkers[T, M](
    routingTable: RDD[Int],
    walkers: RDD[(VertexId, WalkerState[T, M])]
  ): RDD[(VertexId, WalkerState[T, M])] =
    routingTable.zipPartitions {
      walkers.partitionBy(routingTable.partitioner.get)
    } { (_, walker) =>
      walker
    }

  private def walk[T, M](
    walkers: RDD[(VertexId, WalkerState[T, M])],
    walkerConfigBC: Broadcast[WalkerConfig[T]],
    transitionBC: Broadcast[Transition[T, M]]
  ): RDD[(VertexId, WalkerState[T, M])] =
    walkers.mapPartitions(
      { iter =>
        val update     = walkerConfigBC.value.update
        val transition = transitionBC.value

        iter.map { case (ivid, istate) =>
          var vid         = ivid
          var walker      = istate.walker
          var path        = istate.path
          var message     = istate.message
          var done        = istate.done
          var doneLocally = false

          while (!done && !doneLocally) {
            val localData = LocalGraphPartition.data.get(vid)
            localData match {
              case None                              =>
                walker = walker
                  .copy(
                    step = walker.step + 1
                  )
                path = path ++ Array(vid)
                message = None
                done = true
              case Some(LocalData(neighbors, alias)) =>
                val kks  = KnightKingSampling.fromWalkerTransition[T, M](walker, transition)
                val ne   = neighbors(kks.sample(vid, neighbors, message, alias))
                val nvid = ne.dstId
                walker = walker
                  .copy(
                    step = walker.step + 1,
                    data = update(walker, vid, ne)
                  )
                path = path ++ Array(vid)
                message = transition.message(walker, vid, neighbors)
                doneLocally = !LocalGraphPartition.data.contains(nvid)
                vid = nvid
                done = !((new Random()).nextDouble() < transition.extension(walker, vid))
            }
          }
          (vid, WalkerState(walker, path, message, done))
        }
      },
      preservesPartitioning = false
    )

  def run[T, M](
    edgeDirection: EdgeDirection = EdgeDirection.Out
  )(
    graph: Graph[_, Double],
    walkerConfig: WalkerConfig[T],
    transition: Transition[T, M]
  ): RDD[(Long, Array[VertexId])] = {

    val sc = graph.vertices.sparkContext

    val partitioner = graph.vertices.partitioner.getOrElse(new HashPartitioner(graph.vertices.partitions.size))

    val flatten = graph.collectEdges(edgeDirection)

    val transitionBC   = sc.broadcast(transition)
    val walkerConfigBC = sc.broadcast(walkerConfig)

    implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(walkerConfig.parallelism))

    var walkers: Array[RDD[(Long, WalkerState[T, M])]] = initWalkers(flatten.keys, walkerConfigBC, partitioner)

    val routingTable = initRoutingTable(flatten, transitionBC, partitioner).cache()
    routingTable.count()

    val checkpointInterval = sc.getConf
      .getInt("spark.graphx.pregel.checkpointInterval", -1)

    flatten.unpersist()
    graph.unpersist()

    val accFutureFullCompleteWalkers = for (walker <- walkers) yield Future {

      var walkingWalkers = walker

      val walkerCheckpointer =
        new RDDCheckpointer[(Long, WalkerState[T, M])](checkpointInterval, sc, StorageLevel.DISK_ONLY)
      walkerCheckpointer.update(walkingWalkers)
      var numWalkingWalkers  = walkingWalkers.filter { case (_, state) =>
        !state.done
      }.count()

      var accCompleteWalkers = Array.empty[RDD[(Long, Array[VertexId])]]

      var prevWalkers: RDD[(Long, WalkerState[T, M])] = null

      while (numWalkingWalkers > 0) {

        prevWalkers = walkingWalkers

        walkingWalkers = walk(transferWalkers(routingTable, walkingWalkers), walkerConfigBC, transitionBC)
        walkerCheckpointer.update(walkingWalkers)

        val completeWalkers = walkingWalkers.filter { case (_, state) =>
          state.done
        }.mapPartitions(
          _.map { case (_, state) =>
            (state.walker.id, state.path)
          },
          preservesPartitioning = true
        ).persist(StorageLevel.DISK_ONLY)

        if (sc.getCheckpointDir.nonEmpty) completeWalkers.checkpoint()
        completeWalkers.count()

        accCompleteWalkers = accCompleteWalkers ++ Array(completeWalkers)

        walkingWalkers = walkingWalkers.filter { case (_, state) =>
          !state.done
        }
        numWalkingWalkers = walkingWalkers.count()

        prevWalkers.unpersist()

      }

      val fullCompleteWalkers =
        sc.union(ArraySeq.unsafeWrapArray(accCompleteWalkers))
          .partitionBy(partitioner)
          .cache()

      fullCompleteWalkers.count()

      // Cleaning Data
      walkerCheckpointer.deleteAllCheckpoints()
      accCompleteWalkers.foreach { rdd =>
        rdd.getCheckpointFile.foreach(RDDCheckpointer.removeCheckpointFile(_, sc.hadoopConfiguration))
        rdd.unpersist()
      }

      fullCompleteWalkers

    }

    val accFullCompleteWalkers = accFutureFullCompleteWalkers.map(Await.result(_, Duration.Inf))

    routingTable.foreachPartition { iter =>
      LocalGraphPartition.data.clear()
    }
    routingTable.unpersist()

    val res = sc
      .union(ArraySeq.unsafeWrapArray(accFullCompleteWalkers))
      .partitionBy(partitioner)
      .cache()

    res.count()

    accFullCompleteWalkers.foreach(_.unpersist())

    res
  }

}
