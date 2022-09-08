package aruku.sampling

import org.scalatest.funsuite.AnyFunSuite
import org.apache.spark.graphx.{ Edge, Graph, VertexId }
import org.apache.spark.graphx.util.GraphGenerators
import org.apache.spark.SparkContext
import org.apache.spark.graphx.TripletFields
import org.apache.spark.rdd.RDD
import org.apache.spark.HashPartitioner
import org.apache.spark.graphx.EdgeDirection
import org.apache.spark.SparkConf

import aruku.walks._
import aruku._
import aruku.implicits._
import org.apache.spark.graphx.GraphLoader
import org.apache.commons.math3.geometry.spherical.twod.Vertex

class AliasSamplingSuite extends AnyFunSuite {

  test("generate walks") {

    // Start SparkContext
    val sc = SparkContext.getOrCreate(
      new SparkConf()
        .setMaster("local[*]")
        .setAppName("example")
        .set("spark.graphx.pregel.checkpointInterval", "1")
    )
    sc.setCheckpointDir("checkpoint")

    // Generate Graph
    val oriented: Graph[Int, Int] =
      GraphLoader.edgeListFile(sc, "modules/core/src/test/soc-BlogCatalog/soc-BlogCatalog.mtx", numEdgePartitions = 32)
    val graph                     = Graph(oriented.vertices, oriented.edges.union(oriented.reverse.edges).distinct())

    // Node2Vec Configuration
    val numWalkers = graph.vertices.count() * 10
    val numEpochs  = 1
    val walkLength = 80
    val p          = 0.25
    val q          = 0.25

    // Execute Random Walk
    val paths: RDD[(Long, Array[VertexId])] =
      graph.randomWalk(edge => edge.attr.toDouble, EdgeDirection.Out)(
        Node2Vec.config(numWalkers, numEpochs),
        Node2Vec.transition(p, q, walkLength)
      )

    paths.map { case (id, path) => (id, path.toList) }.saveAsTextFile("modules/core/src/test/walks")

  }

}
