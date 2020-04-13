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

import org.apache.spark.graphx.{ Edge, VertexId }

final case class Transition[T, M] private[aruku] (
  extension: (Walker[T], VertexId) => Double,
  static: (VertexId, Edge[Double]) => Double,
  msg: (Walker[T], VertexId, Array[Edge[Double]]) => Option[M],
  dynamic: (Walker[T], VertexId, Edge[Double], Option[M]) => Double,
  upperBound: (VertexId, Array[Edge[Double]]) => Double,
  lowerBound: (VertexId, Array[Edge[Double]]) => Double
)

object Transition {

  def oblivious[T](extension: (Walker[T], VertexId) => Double) = new Transition[T, Nothing](
    extension,
    (_: VertexId, _: Edge[Double]) => 1.0,
    (_: Walker[T], _: VertexId, _: Array[Edge[Double]]) => None,
    (_: Walker[T], _: VertexId, _: Edge[Double], _: Option[Nothing]) => 1.0,
    (_: VertexId, _: Array[Edge[Double]]) => 1.0,
    (_: VertexId, _: Array[Edge[Double]]) => 1.0
  )

  def static[T](
    extension: (Walker[T], VertexId) => Double,
    static: (VertexId, Edge[Double]) => Double
  ) = new Transition[T, Nothing](
    extension,
    static,
    (_: Walker[T], _: VertexId, _: Array[Edge[Double]]) => None,
    (_: Walker[T], _: VertexId, _: Edge[Double], _: Option[Nothing]) => 1.0,
    (_: VertexId, _: Array[Edge[Double]]) => 1.0,
    (_: VertexId, _: Array[Edge[Double]]) => 1.0
  )

  def firstOrder[T](
    extension: (Walker[T], VertexId) => Double,
    static: (VertexId, Edge[Double]) => Double,
    dynamic: (Walker[T], VertexId, Edge[Double]) => Double,
    upperBound: (VertexId, Array[Edge[Double]]) => Double,
    lowerBound: (VertexId, Array[Edge[Double]]) => Double
  ) = new Transition[T, Nothing](
    extension,
    static,
    (_: Walker[T], _: VertexId, _: Array[Edge[Double]]) => None,
    (w: Walker[T], vid: VertexId, e: Edge[Double], _: Option[Nothing]) => dynamic(w, vid, e),
    upperBound,
    lowerBound
  )

  def secondOrder[T, M](
    extension: (Walker[T], VertexId) => Double,
    static: (VertexId, Edge[Double]) => Double,
    msg: (Walker[T], VertexId, Array[Edge[Double]]) => Option[M],
    dynamic: (Walker[T], VertexId, Edge[Double], Option[M]) => Double,
    upperBound: (VertexId, Array[Edge[Double]]) => Double,
    lowerBound: (VertexId, Array[Edge[Double]]) => Double
  ) = new Transition[T, M](
    extension,
    static,
    msg,
    dynamic,
    upperBound,
    lowerBound
  )

}
