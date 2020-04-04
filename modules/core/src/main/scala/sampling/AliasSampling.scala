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

package aruku.sampling

import scala.util.Random
import scala.collection.mutable.MutableList
import scala.collection.mutable.Queue

sealed case class AliasSampling private[aruku] (probs: Array[Double], aliases: Array[Int], random: Random) {

  def next: Int = {
    val column   = random.nextInt(probs.size)
    val coinToss = random.nextDouble() < probs(column)
    if (coinToss) column else aliases(column)
  }

}

object AliasSampling {

  def fromRawProbabilities(rawProbabilities: Array[Double], random: Random = new Random) = {

    val copiedRawProbabilities = MutableList[Double](rawProbabilities: _*)

    val probabilities = new Array[Double](copiedRawProbabilities.size)
    val aliases       = new Array[Int](copiedRawProbabilities.size)

    val small, large = new Queue[Int]

    val average: Double = 1.0 / copiedRawProbabilities.size;

    for (p <- probabilities.indices)
      if (probabilities(p) >= average) large += p else small += p

    while (small.size != 0 && large.size != 0) {
      val less = small.dequeue
      val more = large.dequeue

      probabilities(less) = copiedRawProbabilities(less) * copiedRawProbabilities.size
      aliases(less) = more

      copiedRawProbabilities(more) = (copiedRawProbabilities(more) + copiedRawProbabilities(less)) - average

      if (copiedRawProbabilities(more) >= average) large += more else small += more
    }

    for (i <- large) probabilities(i) = 1.0
    for (i <- small) probabilities(i) = 1.0

    new AliasSampling(probabilities, aliases, random)

  }

}
