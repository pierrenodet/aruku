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

case object DeepWalk {

  def config(numWalkers: Long, numEpochs: Int = 1, parallelism: Int = 1) =
    WalkerConfig.constant(
      numWalkers,
      numEpochs,
      parallelism,
      (current: VertexId) => DeepWalk,
      AtRandom(1.0)
    )

  def transition(walkLength: Long) =
    Transition.static(
      (walker: Walker[DeepWalk.type], _: VertexId) => if (walker.step < walkLength) 1.0 else 0.0,
      (vid: VertexId, edge: Edge[Double]) => edge.attr
    )

}
