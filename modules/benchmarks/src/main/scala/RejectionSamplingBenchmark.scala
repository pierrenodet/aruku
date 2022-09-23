package aruku.benchmarks

import org.openjdk.jmh.annotations._

import java.util.concurrent.TimeUnit
import aruku.sampling.RejectionSampling
import scala.util.Random
import aruku.sampling.AliasMethod

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class RejectionSamplingBenchmark {

  @Param(Array("1000"))
  var size: Int = _

  var array: Array[Int] = _

  @Setup(Level.Trial)
  def init() {
    array = Array.ofDim[Int](size)
  }

  val inf = 0
  val sup = 20
  val f   = (x: Int) => math.exp(-math.pow((x - (sup + inf) / 2), 2))

  @Benchmark
  def baseline() = {

    val random = new Random()
    var i      = 0
    while (i < size) {
      array(i) = random.nextInt(sup - inf + 1) + inf
      i += 1
    }
    array
  }

  @Benchmark
  def domain() = {

    val rs = RejectionSampling.fromDomain(inf, sup)(f, 1)
    var i  = 0
    while (i < size) {
      array(i) = rs.sample()
      i += 1
    }
    array
  }

  val alias = AliasMethod.fromRawProbabilities(Array.fill(20)(1d / 20))

  @Benchmark
  def prior() = {

    val rs = RejectionSampling.fromPrior(alias.next)(f, 1)
    var i  = 0
    while (i < size) {
      array(i) = rs.sample()
      i += 1
    }
    array
  }

}
