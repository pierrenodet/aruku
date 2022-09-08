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

import scala.util.Random
import org.apache.spark.graphx.{ Edge, VertexId }

final case class WalkerConfig[T] private[aruku] (
  numWalkers: Long,
  numEpochs: Int,
  parallelism: Int,
  init: VertexId => T,
  update: (Walker[T], VertexId, Edge[Double]) => T,
  start: StartingStrategy
)

sealed trait StartingStrategy
final case class AtRandom(probability: Double, random: Random = new Random) extends StartingStrategy
final case class FromVertices(vertices: Array[VertexId])                    extends StartingStrategy

object WalkerConfig {
  def updating[T](
    numWalkers: Long,
    numEpochs: Int,
    parallelism: Int,
    init: VertexId => T,
    update: (Walker[T], VertexId, Edge[Double]) => T,
    start: StartingStrategy
  ) = new WalkerConfig[T](numWalkers, numEpochs, parallelism, init, update, start)

  def constant[T](
    numWalkers: Long,
    numEpochs: Int,
    parallelism: Int,
    init: VertexId => T,
    start: StartingStrategy
  ) =
    new WalkerConfig[T](
      numWalkers,
      numEpochs,
      parallelism,
      init,
      (walker: Walker[T], _: VertexId, _: Edge[Double]) => walker.data,
      start
    )
}
