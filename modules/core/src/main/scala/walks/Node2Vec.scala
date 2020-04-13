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

package aruku.walks

import aruku._
import org.apache.spark.graphx.{ Edge, VertexId }

sealed case class Node2Vec private[aruku] (
  previous: VertexId
)

object Node2Vec {

  def config(numWalkers: Long, numEpochs: Int = 1, parallelism: Int = 1) =
    WalkerConfig.dynamic(
      numWalkers,
      numEpochs,
      parallelism,
      (current: VertexId) => Node2Vec(current),
      (walker: Walker[Node2Vec], current: VertexId, next: Edge[Double]) => Node2Vec(current),
      AtRandom(1.0)
    )

  def transition(p: Double, q: Double, walkLength: Long) =
    Transition.secondOrder(
      (walker: Walker[Node2Vec], _: VertexId) => if (walker.step < walkLength) 1.0 else 0.0,
      (vid: VertexId, edge: Edge[Double]) => edge.attr,
      (walker: Walker[Node2Vec], _: VertexId, edges: Array[Edge[Double]]) => Some(edges.map(_.dstId)),
      (
        walker: Walker[Node2Vec],
        current: VertexId,
        next: Edge[Double],
        msg: Option[Array[VertexId]]
      ) =>
        msg match {
          case Some(previousNeighbors) =>
            if (previousNeighbors.contains(next)) {
              1
            } else if (next == walker.data.previous) {
              1 / p
            } else {
              1 / q
            }
          case None => 1.0
        },
      (_: VertexId, _: Array[Edge[Double]]) => math.max(1 / p, math.max(1, 1 / q)),
      (_: VertexId, _: Array[Edge[Double]]) => math.min(1 / p, math.min(1, 1 / q))
    )

}
