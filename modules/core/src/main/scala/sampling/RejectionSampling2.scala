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

sealed abstract case class RejectionSampling2 private (private val nextInt: () => Int)(
  f: Int => Double,
  upperBound: Double,
  lowerBound: Double = 0,
  random: Random = new Random
) {

  def sample(): Int = {

    var hit = false

    var dart = 0.0
    var i    = 0

    while (!hit) {

      i = nextInt()
      dart = random.nextDouble() * upperBound

      if (dart < lowerBound || dart < f(i)) {
        hit = true
      }

    }

    return i

  }

}

object RejectionSampling2 {
  def fromDomain(
    inf: Int,
    sup: Int
  )(f: Int => Double, upperBound: Double, lowerBound: Double = 0, random: Random = new Random): RejectionSampling2 = {

    require(sup > inf)
    require(lowerBound >= 0)
    require(upperBound >= lowerBound)
    require(upperBound > 0)

    new RejectionSampling2(() => random.nextInt(sup - inf + 1) + inf)(f, upperBound, lowerBound, random) {}

  }

  def fromPrior(
    prior: () => Int
  )(f: Int => Double, upperBound: Double, lowerBound: Double = 0, random: Random = new Random): RejectionSampling2 = {

    require(lowerBound >= 0)
    require(upperBound >= lowerBound)
    require(upperBound > 0)

    new RejectionSampling2(prior)(f, upperBound, lowerBound, random) {}

  }
}
