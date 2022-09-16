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

import cats.Eq
import cats.effect.{ IO, Outcome, unsafe }
import cats.effect.testkit.TestContext
import cats.effect.unsafe.IORuntimeConfig
import org.typelevel.catbird.util.{ EqInstances, Rerunnable }
import org.scalacheck.Prop

import scala.annotation.implicitNotFound
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

/**
 * Test helpers mostly taken from the cats-effect IOSpec.
 */
trait Runners { self: EqInstances =>

  implicit val ticker: Ticker = Ticker(TestContext())

  implicit def eqIOA[A: Eq](implicit ticker: Ticker): Eq[IO[A]] =
    Eq.by(unsafeRun(_))

  implicit def rerunnableEq[A](implicit A: Eq[A]): Eq[Rerunnable[A]] =
    Eq.by[Rerunnable[A], IO[A]](rerunnableToIO)

  implicit def boolRunnings(rerunnableB: Rerunnable[Boolean])(implicit ticker: Ticker): Prop =
    Prop(unsafeRun(rerunnableToIO(rerunnableB)).fold(false, _ => false, _.getOrElse(false)))

  def unsafeRun[A](ioa: IO[A])(implicit ticker: Ticker): Outcome[Option, Throwable, A] =
    try {
      var results: Outcome[Option, Throwable, A] = Outcome.Succeeded(None)

      ioa.unsafeRunAsync {
        case Left(t)  => results = Outcome.Errored(t)
        case Right(a) => results = Outcome.Succeeded(Some(a))
      }(unsafe.IORuntime(ticker.ctx, ticker.ctx, scheduler, () => (), IORuntimeConfig()))

      ticker.ctx.advanceAndTick(1.days)

      results
    } catch {
      case t: Throwable =>
        t.printStackTrace()
        throw t
    }

  def scheduler(implicit ticker: Ticker): unsafe.Scheduler =
    new unsafe.Scheduler {
      import ticker.ctx

      def sleep(delay: FiniteDuration, action: Runnable): Runnable = {
        val cancel = ctx.schedule(delay, action)
        new Runnable { def run() = cancel() }
      }

      def nowMillis() = ctx.now().toMillis
      def monotonicNanos() = ctx.now().toNanos
    }

  @implicitNotFound("could not find an instance of Ticker; try using `in ticked { implicit ticker =>`")
  case class Ticker(ctx: TestContext)
}
