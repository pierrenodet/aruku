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

package aruku.embedding

import org.scalatest.FunSuite
import org.apache.spark.graphx.{ Edge, Graph, VertexId }
import org.apache.spark.graphx.util.GraphGenerators
import org.apache.spark.SparkContext
import org.apache.spark.graphx.TripletFields
import org.apache.spark.rdd.RDD
import org.apache.spark.HashPartitioner
import org.apache.spark.graphx.EdgeDirection
import org.apache.spark.SparkConf
import org.apache.spark.mllib.feature.ServerSideGlintWord2Vec

import aruku.walks._
import aruku._
import aruku.implicits._

class RandomWalkEmbeddingSuite extends FunSuite {

  test("generate walks") {

    //Start SparkContext
    val sc = SparkContext.getOrCreate(
      new SparkConf()
        .setMaster("local[*]")
        .setAppName("example")
        .set("spark.graphx.pregel.checkpointInterval", "1")
    )
    sc.setCheckpointDir("checkpoint")

    //Generate Graph
    val numVertices = 100
    val graph: Graph[(Int, Int), Double] =
      GraphGenerators
        .gridGraph(sc, rows = numVertices, cols = numVertices)

    //Node2Vec Configuration
    val numWalkers = 2 * graph.vertices.count()
    val numEpochs  = 2
    val walkLength = 20
    val p          = 0.5
    val q          = 2

    //Embedding Configuration
    val vectorSize     = 10
    val windowSize     = 10
    val subsampleRatio = 1.0
    val minCount       = 0

    //Learn Embeddings
    val model =
      new RandomWalkEmbedding(
        Node2Vec.config(numWalkers, numEpochs),
        Node2Vec.transition(p, q, walkLength),
        new ServerSideGlintWord2Vec()
          .setVectorSize(vectorSize)
          .setMinCount(minCount)
          .setNumParameterServers(1)
          .setSubsampleRatio(subsampleRatio)
          .setWindowSize(windowSize)
      ).fit(graph)

    val vectors = graph.mapVertices { case (vid, _) => model.transform(vid) }.mapEdges(edge => model.transform(edge))

    assert(vectors.vertices.values.take(1)(0).size == vectorSize && vectors.edges.take(1)(0).attr.size == vectorSize)

    vectors.vertices.values.take(10).foreach(vector => println(vector.toArray.mkString(",")))

  }

}
