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

package org.typelevel.catbird.util.effect

import cats.effect.{ ContextShift, IO }
import com.twitter.util.{ ExecutorServiceFuturePool, Future, FuturePool }
import org.scalatest.Outcome
import org.scalatest.funsuite.FixtureAnyFunSuite

import scala.concurrent.ExecutionContext

class ContextShiftingSuite extends FixtureAnyFunSuite with ThreadPoolNamingSupport {

  protected final class FixtureParam {
    val ioPoolName = "io-pool"
    val futurePoolName = "future-pool"

    val ioPool = newNamedThreadPool(ioPoolName)

    val futurePool: ExecutorServiceFuturePool = // threadpool of Future (often managed by a library like finagle-http)
      FuturePool(newNamedThreadPool(futurePoolName))

    def newIO: IO[String] = IO(currentThreadName())

    def newFuture: Future[String] = futurePool.apply {
      // Not 100% sure why but this sleep is needed to reproduce the error. There might be an optimization if the
      // Future is already resolved at some point
      Thread.sleep(200)
      currentThreadName()
    }
  }

  test("After resolving the Future with futureToAsync stay on the Future threadpool") { f =>
    implicit val contextShift: ContextShift[IO] = // threadpool of IO (often provided by IOApp)
      IO.contextShift(ExecutionContext.fromExecutor(f.ioPool))

    val (futurePoolName, ioPoolName) = (for {
      futurePoolName <- futureToAsync[IO, String](f.newFuture)

      ioPoolName <- f.newIO
    } yield (futurePoolName, ioPoolName)).start(contextShift).flatMap(_.join).unsafeRunSync()

    assert(futurePoolName == f.futurePoolName)
    assert(ioPoolName == f.futurePoolName) // Uh oh, this is likely not what the user wants
  }

  test("After resolving the Future with futureToAsyncAndShift shift back to the threadpool of ContextShift[F]") { f =>
    implicit val contextShift: ContextShift[IO] = // threadpool of IO (often provided by IOApp)
      IO.contextShift(ExecutionContext.fromExecutor(f.ioPool))

    // If you'd use `futureToAsync` here instead, this whole thing would sometimes stay on the future-pool
    val (futurePoolName, ioPoolName) = (for {
      futurePoolName <- futureToAsyncAndShift[IO, String](f.newFuture)

      ioPoolName <- f.newIO
    } yield (futurePoolName, ioPoolName))
      .start(contextShift) // start the computation on the default threadpool...
      .flatMap(_.join) // ...then block until we have the results
      .unsafeRunSync()

    assert(futurePoolName == f.futurePoolName)
    assert(ioPoolName == f.ioPoolName)
  }

  override protected def withFixture(test: OneArgTest): Outcome = withFixture(test.toNoArgTest(new FixtureParam))
}
