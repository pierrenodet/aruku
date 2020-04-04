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

import org.scalatest.FunSuite
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

class WalkSuite extends FunSuite {

  test("can generate paths") {

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
      graph.randomWalk(edge => edge.attr.toDouble, EdgeDirection.Out)(
        Node2Vec.config(numWalkers),
        Node2Vec.transition(p, q, walkLength)
      )

    //Print 10 first Random Walks
    paths.take(10).foreach { case (k, v) => println(k, v.mkString(",")) }
    assert(paths.count() == numWalkers)
    assert(paths.collect().map { case (_, path) => path.size }.contains(walkLength))

  }

}
