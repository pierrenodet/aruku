package aruku.benchmarks

import org.openjdk.jmh.annotations._

import java.util.concurrent.TimeUnit
import aruku.sampling.RejectionSampling
import scala.util.Random
import aruku.sampling.AliasMethod

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class AliasMethodBenchmark {

  @Param(Array("100", "1000", "10000"))
  var size: Int = _

  var array: Array[Int]    = _
  var proba: Array[Double] = _
  var alias: AliasMethod   = _

  @Setup(Level.Trial)
  def init() {
    array = Array.ofDim[Int](size)
    proba = Array.fill(size)(1d / size)
    alias = AliasMethod.fromRawProbabilities(proba)
  }

  @Benchmark
  def baseline() = {

    val random = new Random()
    var i      = 0
    while (i < size) {
      array(i) = random.nextInt(size + 1)
      i += 1
    }
    array
  }

  @Benchmark
  def aliasN() = {

    var i = 0
    while (i < size) {
      array(i) = alias.next()
      i += 1
    }
    array
  }

  @Benchmark
  def aliasB() = AliasMethod.fromRawProbabilities(proba)

}
