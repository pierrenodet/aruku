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
import aruku.partition.LocalData
import aruku.partition.LocalGraphPartition
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
import org.scalacheck.Gen
import org.scalacheck.Shrink
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.language.postfixOps

class WalkSuite extends AnyFunSuite with BeforeAndAfterAll with ScalaCheckPropertyChecks {

  import org.scalacheck.Shrink.shrinkAny

  var sc: SparkContext = _

  override def beforeAll() {

    sc = SparkContext.getOrCreate(
      new SparkConf()
        .setMaster("local[*]")
        .setAppName("example")
        .set("spark.graphx.pregel.checkpointInterval", "1")
    )
    sc.setCheckpointDir("checkpoint")
    sc.setLogLevel("ERROR")

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

    val parallelism = 2
    val graph       = Graph(sc.makeRDD(vertices, parallelism), sc.makeRDD(edges)).cache()

    val numWalkers = 10000
    val numEpochs  = 1
    val walkLength = 3

    val pgen = Gen.chooseNum(0.1, 0.9)
    val qgen = Gen.chooseNum(0.1, 0.9)

    val gen = for {
      p <- pgen
      q <- qgen
    } yield (p, q)

    forAll(gen) { case (p, q) =>
      val paths: RDD[(Long, Array[VertexId])] = graph.randomWalk(edge => edge.attr.toDouble, EdgeDirection.Out)(
        Node2Vec.config(numWalkers, numEpochs),
        Node2Vec.transition(p, q, walkLength)
      )

      assert(paths.count() == numWalkers)
      assert(
        paths.map { case (_, path) =>
          (path.head, 1)
        }.countByKey() == Map(
          (previousVertice     -> numWalkers / 4),
          (starVertice         -> numWalkers / 4),
          (prevNeighborVertice -> numWalkers / 4),
          (aloneVertice        -> numWalkers / 4)
        )
      )

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
      val distrib  = proba.map { case (k, v) => (k, v / sumProba) }

      val precision = 5e-2
      assert(estimate(previousVertice) === distrib(previousVertice) +- precision)
      assert(estimate.getOrElse(starVertice, 0.0) === distrib(starVertice) +- precision)
      assert(estimate(prevNeighborVertice) === distrib(prevNeighborVertice) +- precision)
      assert(estimate(aloneVertice) === distrib(aloneVertice) +- precision)
    }

    graph.unpersist()

  }

  test("personalized page rank stochastic length") {

    // Generate Graph
    val numVertices             = 1000
    val parallelism             = 2
    val graph: Graph[Long, Int] =
      GraphGenerators
        .logNormalGraph(sc, numVertices = numVertices, numEParts = parallelism)

    val gen = Gen.chooseNum(0.1, 0.9)

    forAll(gen) { pi =>
      val numWalkers = 100000
      val numEpochs  = 1

      val paths: RDD[(Long, Array[VertexId])] = graph.randomWalk(edge => edge.attr.toDouble, EdgeDirection.Either)(
        PersonalizedPageRank.config(numWalkers, numEpochs),
        PersonalizedPageRank.transition(pi)
      )
      val numPaths                            = paths.count()

      assert(numPaths == numWalkers)
      assert(paths.filter(_._2.isEmpty).count() == 0)
      assert(paths.filter(_._2.size == 1).count().toDouble / numPaths === (1 - pi) +- 1e-2)
      assert(paths.map(_._2.size).sum() / numPaths === 1 + (pi / (1 - pi)) +- 1e-1)
    }

  }

  test("random walk never go twice in the same node") {

    // Generate Graph
    val numVertices             = 1000
    val parallelism             = 2
    val graph: Graph[Long, Int] =
      GraphGenerators
        .logNormalGraph(sc, numVertices = numVertices, numEParts = parallelism)
        .removeSelfEdges()

    val numWalkers = 1000
    val walkLength = 10

    val paths: RDD[(Long, Array[VertexId])] =
      graph.randomWalk(edge => edge.attr.toDouble, EdgeDirection.Out)(
        DeepWalk.config(numWalkers),
        DeepWalk.transition(walkLength)
      )

    assert(
      paths
        .map(_._2)
        .filter(_.size >= 2)
        .collect()
        .forall(path => path.toList.sliding(2).collect { case head :: next => head != next.head }.forall(identity))
    )

  }

  test("walker config with FromVertices strategy generates walkers in chosen vertices only") {

    // Generate Graph
    val numVertices             = 100
    val parallelism             = 2
    val graph: Graph[Long, Int] =
      GraphGenerators
        .logNormalGraph(sc, numVertices = numVertices, numEParts = parallelism)

    val numWalkers     = numVertices * 2
    val chosenVertices = graph.vertices.keys.take(10)
    val walkLength     = 10

    val paths: RDD[(Long, Array[VertexId])] =
      graph.randomWalk(edge => edge.attr.toDouble, EdgeDirection.Out)(
        WalkerConfig.constant(numWalkers, 1, 1, _ => DeepWalk, FromVertices(chosenVertices)),
        DeepWalk.transition(walkLength)
      )

    assert(
      paths
        .map(_._2.head)
        .collect()
        .forall(chosenVertices.contains)
    )

  }

  test("proper data cleanup after random walk is finished") {

    // Clean previous persisted RDDs
    sc.getPersistentRDDs.values.foreach(_.unpersist())

    // Generate Graph
    val numVertices             = 100
    val parallelism             = 2
    val graph: Graph[Long, Int] =
      GraphGenerators
        .logNormalGraph(sc, numVertices = numVertices, numEParts = parallelism)
    // GraphGenerators automatically persist graphs
    graph.unpersist()

    val numWalkers = numVertices * 2
    val walkLength = 10

    val paths: RDD[(Long, Array[VertexId])] =
      graph.randomWalk(edge => edge.attr.toDouble, EdgeDirection.Out)(
        DeepWalk.config(numWalkers),
        DeepWalk.transition(walkLength)
      )
    // Computed paths are automatically persisted
    paths.unpersist()

    assert(LocalGraphPartition.data.isEmpty)
    assert(sc.getPersistentRDDs.isEmpty)

  }

}
