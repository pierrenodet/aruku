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

package aruku.sampling

import aruku._
import org.apache.spark.graphx.Edge
import org.apache.spark.graphx.VertexId

import scala.collection.mutable.Queue
import scala.util.Random

final case class KnightKingSampling[M] private[aruku] (
  dynamic: (VertexId, Edge[Double], Option[M]) => Double,
  upperBound: (VertexId, Array[Edge[Double]]) => Double,
  lowerBound: (VertexId, Array[Edge[Double]]) => Double
) {

  def sample(current: VertexId, neighbors: Array[Edge[Double]], message: Option[M], alias: AliasMethod): Int = {

    val f  = (i: Int) => dynamic(current, neighbors(i), message)
    val ub = upperBound(current, neighbors)
    val lb = lowerBound(current, neighbors)

    RejectionSampling.fromPrior(alias.next)(f, ub, lb).sample()
  }

}

object KnightKingSampling {

  def fromWalkerTransition[T, M](walker: Walker[T], transition: Transition[T, M]) = {

    val dynamic    = (vid: VertexId, e: Edge[Double], m: Option[M]) => transition.dynamic(walker, vid, e, m)
    val upperBound = transition.upperBound
    val lowerBound = transition.lowerBound

    new KnightKingSampling(dynamic, upperBound, lowerBound)

  }

}
