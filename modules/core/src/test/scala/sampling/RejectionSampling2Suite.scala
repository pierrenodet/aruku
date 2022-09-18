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

import org.apache.spark.graphx.{ Edge, Graph, VertexId }
import org.apache.spark.graphx.util.GraphGenerators
import org.apache.spark.SparkContext
import org.apache.spark.graphx.TripletFields
import org.apache.spark.rdd.RDD
import org.apache.spark.HashPartitioner
import org.apache.spark.graphx.EdgeDirection
import org.apache.spark.SparkConf

import aruku.walks._
import aruku._
import aruku.implicits._
import org.apache.spark.graphx.GraphLoader
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.language.postfixOps
import org.scalatest.matchers.should.Matchers._
import org.scalatest.funsuite.AnyFunSuite
import org.scalacheck.Shrink
import scala.util.Random

class RejectionSampling2Suite extends AnyFunSuite with ScalaCheckPropertyChecks {

  import org.scalacheck.Shrink.shrinkAny

  def intervalGen(minStart: Int, maxStart: Int, avgSize: Double) = {
    val infGen  = Gen.chooseNum(minStart, maxStart)
    val sizeGen = Gen.poisson(avgSize)
    for {
      inf  <- infGen
      size <- sizeGen
    } yield (inf, inf + size + 1)
  }

  test("acceptance-rejection sampling produces right samples") {

    forAll(intervalGen(-10, 10, 5d)) { case (inf, sup) =>
      val domain = Range.inclusive(inf, sup)
      val f      = domain.map(i => (i, math.abs(i % 2).toDouble)).toMap
      val rs     = RejectionSampling2.fromDomain(inf, sup)(f, 1)

      val n = 10000

      val occurences = List.fill(n)(rs.sample()).groupMapReduce(identity)(_ => 1.0 / n)(_ + _)
      val max        = occurences.values.max
      val res        = domain.map(i => (i, occurences.getOrElse(i, 0d) / max))

      val precision = 1e-1

      assert(res.forall { case (k, v) => f(k) === v +- precision })
    }
  }

  test("lowerbound equals 1.0 produces uniform samples") {

    forAll(intervalGen(-10, 10, 5d)) { case (inf, sup) =>
      val domain = Range.inclusive(inf, sup)
      val f      = (x: Int) => math.exp(-math.pow((x - (sup + inf) / 2), 2))
      val rs     = RejectionSampling2.fromDomain(inf, sup)(f, 1, 1)

      val n = 1000

      val samples    = List.fill(n)(rs.sample())
      val occurences = samples.groupMapReduce(identity)(_ => 1.0 / n)(_ + _)

      val precision = 1e-1

      assert(occurences.values.forall(o => o === (1d / domain.size) +- precision))
    }
  }

  test("domain and no prior is equivalent") {

    forAll(intervalGen(-10, 10, 5d)) { case (inf, sup) =>
      val f           = (x: Int) => math.exp(-math.pow((x - (sup + inf) / 2), 2))
      val domain      = RejectionSampling2.fromDomain(inf, sup)(f, 1, 0, new Random(1))
      val priorRandom = new Random(1)
      val prior       = RejectionSampling2.fromPrior(() => priorRandom.nextInt(sup - inf + 1) + inf)(f, 1, 0, priorRandom)

      val n = 100

      val domainSample = List.fill(n)(domain.sample())
      val priorSample  = List.fill(n)(prior.sample())

      assert(domainSample.zip(priorSample).forall { case (d, p) => d == p })
    }
  }

  test("different seeds produce different outputs") {

    forAll(intervalGen(-10, 10, 5d)) { case (inf, sup) =>
      val f       = (x: Int) => math.exp(-math.pow((x - (sup + inf) / 2), 2))
      val domain  = RejectionSampling2.fromDomain(inf, sup)(f, 1)
      val domain2 = RejectionSampling2.fromDomain(inf, sup)(f, 1)

      val n = 100

      val domainSample  = List.fill(n)(domain.sample())
      val domainSample2 = List.fill(n)(domain2.sample())

      assert(domainSample.zip(domainSample2).exists { case (d, p) => d != p })
    }
  }

  test("sampled elements come from domain") {

    forAll(intervalGen(-10, 10, 5d)) { case (inf, sup) =>
      val f      = (x: Int) => math.exp(-math.pow((x - (sup + inf) / 2), 2))
      val domain = RejectionSampling2.fromDomain(inf, sup)(f, 1)

      val n = 100

      val domainSample = List.fill(n)(domain.sample())

      assert(domainSample.forall(s => (s >= inf) && (s <= sup)))
    }
  }

  test("wrong domain should produce exception") {

    assertThrows[IllegalArgumentException](RejectionSampling2.fromDomain(1, -1)(_.toDouble, 1))

  }

  test("wrong lowerbound should produce exception") {

    assertThrows[IllegalArgumentException](RejectionSampling2.fromDomain(1, -1)(identity, 1, 1))

  }

}
