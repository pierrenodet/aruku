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
import scala.collection.mutable.ArrayDeque

sealed abstract case class AliasMethod private (probs: Array[Double], aliases: Array[Int], random: Random) {

  def next(): Int = {
    val column   = random.nextInt(probs.size)
    val coinToss = random.nextDouble() < probs(column)
    if (coinToss) column else aliases(column)
  }

}

object AliasMethod {

  def fromRawProbabilities(rawProbabilities: Array[Double], random: Random = new Random) = {

    require(!rawProbabilities.isEmpty)

    val rawprobs = rawProbabilities.clone()

    val sum = rawprobs.sum
    for (i <- rawprobs.indices)
      rawprobs(i) /= sum

    val probs   = new Array[Double](rawprobs.size)
    val aliases = new Array[Int](rawprobs.size)

    val small, large = new Queue[Int]

    val n = rawprobs.size;

    for (i <- rawprobs.indices)
      rawprobs(i) *= n

    for (i <- rawprobs.indices)
      if (rawprobs(i) >= 1) large += i else small += i

    while (small.size != 0 && large.size != 0) {
      val less = small.dequeue()
      val more = large.dequeue()

      probs(less) = rawprobs(less)
      aliases(less) = more

      rawprobs(more) = (rawprobs(more) + rawprobs(less)) - 1

      if (rawprobs(more) >= 1) large += more else small += more
    }

    for (i <- large) probs(i) = 1.0
    for (i <- small) probs(i) = 1.0

    new AliasMethod(probs, aliases, random) {}

  }

}
