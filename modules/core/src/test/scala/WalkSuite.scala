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

import aruku._
import aruku.implicits._
import aruku.walks._
import org.apache.spark.HashPartitioner
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.graphx.Edge
import org.apache.spark.graphx.EdgeDirection
import org.apache.spark.graphx.EdgeRDD
import org.apache.spark.graphx.Graph
import org.apache.spark.graphx.VertexId
import org.apache.spark.graphx.util.GraphGenerators
import org.apache.spark.rdd.RDD
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._

import scala.language.postfixOps

class WalkSuite extends AnyFunSuite with BeforeAndAfterAll {

  var sc: SparkContext = _

  override def beforeAll() {

    sc = SparkContext.getOrCreate(
      new SparkConf()
        .setMaster("local[*]")
        .setAppName("example")
        .set("spark.graphx.pregel.checkpointInterval", "1")
    )
    sc.setCheckpointDir("checkpoint")

  }

  override def afterAll() {
    sc.stop()
  }

  test("node2vec dynamic transition is respected") {

    val previousVertice     = 1L
    val starVertice         = 2L
    val prevNeighborVertice = 3L
    val aloneVertice        = 4L
    val vertices            = Seq(previousVertice, starVertice, prevNeighborVertice, aloneVertice).map(v => (v, v.toString()))
    val edges               =
      vertices.filter(_._1 != starVertice).map { case (v, n) => Edge(starVertice, v.toLong, 1.0) } ++ Seq(
        Edge(previousVertice, prevNeighborVertice, 1.0),
        Edge(previousVertice, starVertice, 1.0)
      )

    val parallelism = 4
    val graph       = Graph(sc.makeRDD(vertices, parallelism), sc.makeRDD(edges))

    val numWalkers = vertices.size * 5000
    val numEpochs  = 1
    val walkLength = 3
    val p          = 0.5
    val q          = 2

    val paths: RDD[(Long, Array[VertexId])] = graph.randomWalk(edge => edge.attr.toDouble, EdgeDirection.Out)(
      Node2Vec.config(numWalkers, numEpochs),
      Node2Vec.transition(p, q, walkLength)
    )

    assert(paths.count() == numWalkers)

    val startsWithPrevVerticeAndGoToStar = paths.filter { case (_, p) =>
      p(0) == previousVertice && p(1) == starVertice
    }
    val trueNumWalkers                   = startsWithPrevVerticeAndGoToStar.count()
    val ends                             = startsWithPrevVerticeAndGoToStar.map(_._2.last)
    val estimate                         = ends
      .map(e => (e, 1.0 / trueNumWalkers))
      .reduceByKeyLocally(_ + _)

    val proba    = Map(
      (previousVertice, 1.0 / p),
      (starVertice, 0.0),
      (prevNeighborVertice, 1.0),
      (aloneVertice, 1.0 / q)
    )
    val sumProba = proba.values.sum
    val distrib  = proba.view.mapValues(_ / sumProba).toMap

    val precision = 1e-2
    assert(
      distrib.keys.forall(i => distrib(i) === estimate.getOrElse(i, 0.0) +- precision)
    )
  }

//   test("generate walks") {

//     // Start SparkContext
//     val sc = SparkContext.getOrCreate(
//       new SparkConf()
//         .setMaster("local[*]")
//         .setAppName("example")
//         .set("spark.graphx.pregel.checkpointInterval", "1")
//     )
//     sc.setCheckpointDir("checkpoint")

//     // Generate Graph
//     val numVertices             = 1000
//     val graph: Graph[Long, Int] =
//       GraphGenerators
//         .logNormalGraph(sc, numVertices = numVertices)
//     val full                    = Graph(graph.vertices, graph.edges.union(graph.edges.reverse))

//     // Node2Vec Configuration
//     val numWalkers = 5000
//     val numEpochs  = 2
//     val walkLength = 10
//     val p          = 0.5
//     val q          = 2

//     // Execute Random Walk
//     val paths =
//       graph.randomWalk(edge => edge.attr.toDouble, EdgeDirection.Out)(
//         Node2Vec.config(numWalkers, numEpochs),
//         Node2Vec.transition(p, q, walkLength)
//       )

//     val sizes = paths.collect().map { case (_, path) => path.size }

//     assert(paths.count() == numWalkers && sizes.map(size => math.abs(size - walkLength)).sum < 0.01 * numWalkers)

//     // Second API with a RDD without partitioner
//     val paths2 = RandomWalk.run(EdgeDirection.Out)(
//       graph.mapEdges(_.attr.toDouble),
//       Node2Vec.config(numWalkers, numEpochs),
//       Node2Vec.transition(p, q, walkLength)
//     )

//     val sizes2 = paths2.collect().map { case (_, path) => path.size }

//     assert(paths2.count() == numWalkers && sizes2.map(size => math.abs(size - walkLength)).sum < 0.01 * numWalkers)

//   }

}
