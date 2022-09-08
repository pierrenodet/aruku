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

import scala.util.Random
import scala.collection.mutable.Queue
import org.apache.spark.graphx.{ Edge, VertexId }
import aruku._

sealed case class RejectionSampling[T, M] private[aruku] (
  dynamic: (VertexId, Edge[Double], Option[M]) => Double,
  upperBound: (VertexId, Array[Edge[Double]]) => Double,
  lowerBound: (VertexId, Array[Edge[Double]]) => Double,
  random: Random
) {

  def next(current: VertexId, neighbors: Array[Edge[Double]], message: Option[M], alias: AliasSampling): Int = {

    val ub = upperBound(current, neighbors)
    val lb = lowerBound(current, neighbors)

    var hit = false

    var dart = 0.0
    var nidx = 0

    while (!hit) {

      dart = random.nextDouble() * ub
      nidx = alias.next

      if (dart < lb || dart < dynamic(current, neighbors(nidx), message)) {
        hit = true
      }

    }

    nidx

  }

}

object RejectionSampling {

  def fromWalkerTransition[T, M](walker: Walker[T], transition: Transition[T, M], random: Random = new Random) = {

    val dynamic    = Function.uncurried(transition.dynamic.curried.apply(walker))
    val upperBound = transition.upperBound
    val lowerBound = transition.lowerBound

    new RejectionSampling(dynamic, upperBound, lowerBound, random)

  }

}
