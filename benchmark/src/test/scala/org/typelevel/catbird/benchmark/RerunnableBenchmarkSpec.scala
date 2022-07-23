/*
 * Copyright 2022 Typelevel
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

package org.typelevel.catbird.benchmark

import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec

class RerunnableBenchmarkSpec extends AnyFlatSpec with BeforeAndAfter {
  val benchmark: RerunnableBenchmark = new RerunnableBenchmark
  val sum = benchmark.numbers.sum

  before(benchmark.initPool())
  after(benchmark.shutdownPool())

  "The benchmark" should "correctly calculate the sum using futures" in {
    assert(benchmark.sumIntsF === sum)
  }

  it should "correctly calculate the sum using futures and future pools" in {
    assert(benchmark.sumIntsPF === sum)
  }

  it should "correctly calculate the sum using rerunnables" in {
    assert(benchmark.sumIntsR === sum)
  }

  it should "correctly calculate the sum using rerunnables and future pools" in {
    assert(benchmark.sumIntsPR === sum)
  }
}
