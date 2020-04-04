---
id: example
title: "Example"
------

Let's generate a graph with GraphX and run node2vec on it !

```scala
import org.apache.spark.graphx.util.GraphGenerators
import org.apache.spark.graphx._
import org.apache.spark._

import aruku.walks._
import aruku._
import aruku.implicits._

object Main extends App {

    //Start SparkContext
    val sc = SparkContext.getOrCreate(
      new SparkConf()
        .setMaster("local[*]")
        .setAppName("example")
        .set("spark.graphx.pregel.checkpointInterval", "1")
    )
    sc.setCheckpointDir("checkpoint")

    //Generate Graph
    val numVertices = 150000
    val graph: Graph[Long, Int] =
      GraphGenerators
        .logNormalGraph(sc, numVertices = numVertices)

    //Node2Vec Configuration
    val numWalkers = 150000
    val walkLength = 80
    val p          = 0.5
    val q          = 2

    //Execute Random Walk
    val paths =
      graph.randomWalk(edge => edge.attr.toDouble, EdgeDirection.Out)
    (Node2Vec.config(numWalkers), Node2Vec.transition(p, q, walkLength))

    //Print 10 first Random Walks
    paths.take(10).foreach { 
        case (walkerId, path) => println(walkerId, path.mkString(",")) 
    }

}

```