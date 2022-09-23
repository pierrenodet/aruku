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

package aruku.walks

import aruku._
import org.apache.spark.graphx.Edge
import org.apache.spark.graphx.VertexId

final case class Node2Vec private[aruku] (
  val previous: VertexId
) extends AnyVal

object Node2Vec {

  def config(numWalkers: Long, numEpochs: Int = 1, parallelism: Int = 1) =
    WalkerConfig.updating(
      numWalkers,
      numEpochs,
      parallelism,
      (current: VertexId) => Node2Vec(current),
      (_: Walker[Node2Vec], current: VertexId, _: Edge[Double]) => Node2Vec(current),
      AtRandom(1.0)
    )

  def transition(p: Double, q: Double, walkLength: Long) =
    Transition.secondOrder(
      (walker: Walker[Node2Vec], _: VertexId) => if (walker.step < walkLength) 1.0 else 0.0,
      (_: VertexId, edge: Edge[Double]) => edge.attr,
      (walker: Walker[Node2Vec], _: VertexId, edges: Array[Edge[Double]]) => Some(edges),
      (
        walker: Walker[Node2Vec],
        current: VertexId,
        next: Edge[Double],
        msg: Option[Array[Edge[Double]]]
      ) =>
        msg match {
          case Some(previousNeighbors) =>
            val dst = next.dstId
            if (dst == walker.data.previous) {
              1.0 / p
            } else if (previousNeighbors.exists(_.dstId == dst)) {
              1.0
            } else {
              1.0 / q
            }
          case None                    => 1.0
        },
      (_: VertexId, _: Array[Edge[Double]]) => math.max(1.0 / p, math.max(1.0, 1.0 / q)),
      (_: VertexId, _: Array[Edge[Double]]) => math.min(1.0 / p, math.min(1.0, 1.0 / q))
    )

}
