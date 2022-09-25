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

import org.apache.spark.HashPartitioner
import org.apache.spark.SparkException
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

class WalkOps[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED]) extends Serializable {

  def collectNeighborEdges(edgeDirection: EdgeDirection): VertexRDD[Array[Edge[ED]]] = {
    val nbrs = edgeDirection match {
      case EdgeDirection.Either =>
        graph.aggregateMessages[Array[Edge[ED]]](
          ctx => {
            ctx.sendToSrc(Array(new Edge(ctx.srcId, ctx.dstId, ctx.attr)))
            ctx.sendToDst(Array(new Edge(ctx.dstId, ctx.srcId, ctx.attr)))
          },
          (a, b) => a ++ b,
          TripletFields.EdgeOnly
        )
      case EdgeDirection.In     =>
        graph.aggregateMessages[Array[Edge[ED]]](
          ctx => ctx.sendToDst(Array(new Edge(ctx.dstId, ctx.srcId, ctx.attr))),
          (a, b) => a ++ b,
          TripletFields.EdgeOnly
        )
      case EdgeDirection.Out    =>
        graph.aggregateMessages[Array[Edge[ED]]](
          ctx => ctx.sendToSrc(Array(new Edge(ctx.srcId, ctx.dstId, ctx.attr))),
          (a, b) => a ++ b,
          TripletFields.EdgeOnly
        )
      case EdgeDirection.Both   =>
        throw new SparkException(
          "collectNeighborEdges does not support EdgeDirection.Both. Use" +
            "EdgeDirection.Either instead."
        )
    }
    graph.vertices.leftJoin(nbrs)((vid, vdata, nbrsOpt) => nbrsOpt.getOrElse(Array.empty[Edge[ED]]))
  }

  def randomWalk[T, M](preprocess: Edge[ED] => Double, edgeDirection: EdgeDirection = EdgeDirection.Out)(
    walkerConfig: WalkerConfig[T],
    transition: Transition[T, M]
  ): RDD[(Long, Array[VertexId])] =
    RandomWalk.run(edgeDirection)(
      graph.mapEdges(preprocess),
      walkerConfig,
      transition
    )

}

trait ToWalkOps {

  implicit def graphToWalkOps[VD: ClassTag, ED: ClassTag](g: Graph[VD, ED]): WalkOps[VD, ED] = new WalkOps[VD, ED](g)

}
