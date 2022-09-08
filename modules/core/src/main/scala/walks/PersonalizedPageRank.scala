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

import org.apache.spark.graphx.{ Edge, VertexId }

case object PersonalizedPageRank {

  def config(numWalkers: Long, numEpochs: Int = 1, parallelism: Int = 1) =
    WalkerConfig.constant(
      numWalkers,
      numEpochs,
      parallelism,
      (_: VertexId) => PersonalizedPageRank,
      AtRandom(1.0)
    )

  def transition(pi: Double) =
    Transition.static(
      (_: Walker[PersonalizedPageRank.type], _: VertexId) => pi,
      (_: VertexId, edge: Edge[Double]) => edge.attr
    )

}
