package org.typelevel.catbird.benchmark

import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec

class RerunnableBenchmarkSpec extends AnyFlatSpec with BeforeAndAfter {
  val benchmark: RerunnableBenchmark = new RerunnableBenchmark
  val sum = benchmark.numbers.sum

  before(benchmark.initPool())
  after(benchmark.shutdownPool())

  "The benchmark" should "correctly calculate the sum using futures" in
    assert(benchmark.sumIntsF === sum)

  it should "correctly calculate the sum using futures and future pools" in
    assert(benchmark.sumIntsPF === sum)

  it should "correctly calculate the sum using rerunnables" in
    assert(benchmark.sumIntsR === sum)

  it should "correctly calculate the sum using rerunnables and future pools" in
    assert(benchmark.sumIntsPR === sum)
}
