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
import aruku.implicits._
import org.scalacheck.Gen
import org.scalacheck.Shrink
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.language.postfixOps

class AliasMethodSuite extends AnyFunSuite with ScalaCheckPropertyChecks {

  import org.scalacheck.Shrink.shrinkAny

  test("can reconstruct input probs from alias probs and aliases") {

    val gen = Gen.listOfN(10, Gen.chooseNum[Double](0, 5))

    forAll(gen) { probs =>
      val sum        = probs.sum
      val normalized = probs.map(_ / sum)
      val alias      = AliasMethod.fromRawProbabilities(probs.toArray)
      val comp       = alias.probs.map(1.0 - _)

      var reconstructed = alias.probs.zipWithIndex.map(_.swap).toMap

      comp.zipWithIndex.foreach { case (c, i) =>
        reconstructed = reconstructed.updated(alias.aliases(i), reconstructed.getOrElse(alias.aliases(i), 0d) + c)
      }

      val sumReconstructed        = reconstructed.values.sum
      val normalizedReconstructed = reconstructed.toSeq.sortBy(_._1).map(_._2 / sumReconstructed)

      val precision = 1e-12

      assert(normalizedReconstructed.zip(normalized).forall { case (r, n) => r === n +- precision })
    }
  }

  test("alias sampling reproduces input probabilities") {

    val gen = Gen.listOfN(10, Gen.chooseNum[Double](0, 5))

    forAll(gen) { probs =>
      val sum        = probs.sum
      val normalized = probs.map(_ / sum)
      val alias      = AliasMethod.fromRawProbabilities(probs.toArray)

      val size     = 100000
      val sampled  = List.fill(size)(alias.next())
      val bincount = sampled.groupBy(identity).map { case (i, o) => (i, o.size.toDouble / size) }
      val res      = probs.indices.map(bincount.getOrElse(_, 0d)).toList

      val precision = 1e-2

      assert(res.zip(normalized).forall { case (r, n) => r === n +- precision })
    }
  }

  test("alias sampling throws error with empty proba") {

    assertThrows[IllegalArgumentException](AliasMethod.fromRawProbabilities(Array.empty[Double]))

  }

}
