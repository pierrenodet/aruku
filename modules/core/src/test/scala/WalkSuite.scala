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

// package aruku

// import org.scalatest.funsuite.AnyFunSuite
// import org.apache.spark.graphx.{ Edge, Graph, VertexId }
// import org.apache.spark.graphx.util.GraphGenerators
// import org.apache.spark.SparkContext
// import org.apache.spark.graphx.TripletFields
// import org.apache.spark.rdd.RDD
// import org.apache.spark.HashPartitioner
// import org.apache.spark.graphx.EdgeDirection
// import org.apache.spark.SparkConf

// import aruku.walks._
// import aruku._
// import aruku.implicits._

// class WalkSuite extends AnyFunSuite {

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

// }
