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

import com.twitter.util.{ Await, Future, FuturePool }
import org.typelevel.catbird.util.Rerunnable
import java.util.concurrent.{ ExecutorService, Executors, TimeUnit }
import org.openjdk.jmh.annotations._

/**
 * Compare the performance of `Rerunnable` against ordinary `Future`s.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/jmh:run -i 20 -wi 10 -f 2 -t 1 org.typelevel.catbird.benchmark.RerunnableBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class RerunnableBenchmark {
  val count: Int = 100000
  val numbers: IndexedSeq[Int] = 0 to count
  var es: ExecutorService = _
  var pool: FuturePool = _

  @Setup
  def initPool(): Unit = {
    es = Executors.newFixedThreadPool(4)
    pool = FuturePool(es)
  }

  @TearDown
  def shutdownPool(): Unit = es.shutdown()

  @Benchmark
  def sumIntsF: Int = Await.result(
    numbers.foldLeft(Future(0)) { case (acc, i) =>
      acc.flatMap(prev => Future(prev + i))
    }
  )

  @Benchmark
  def sumIntsR: Int = Await.result(
    numbers
      .foldLeft(Rerunnable(0)) { case (acc, i) =>
        acc.flatMap(prev => Rerunnable(prev + i))
      }
      .run
  )

  @Benchmark
  def sumIntsPF: Int = Await.result(
    numbers.foldLeft(pool(0)) { case (acc, i) =>
      acc.flatMap(prev => pool(prev + i))
    }
  )

  @Benchmark
  def sumIntsPR: Int = Await.result(
    numbers
      .foldLeft(Rerunnable.withFuturePool(pool)(0)) { case (acc, i) =>
        acc.flatMap(prev => Rerunnable.withFuturePool(pool)(prev + i))
      }
      .run
  )
}
