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

import cats.effect.{ Clock, Timer }
import org.typelevel.catbird.util.Rerunnable
import com.twitter.util.Future
import com.twitter.util
import scala.Unit

import scala.concurrent.duration.FiniteDuration

/**
 * Can be used to construct a `cats.effect.Timer` instance for `Rerunnable` which let's you delay execution or retrieve
 * the current time via `RerunnableClock`.
 *
 * Usage:
 * {{{
 *   // In a Finagle application
 *   implicit val timer: Timer[Rerunnable] = RerunnableTimer(com.twitter.finagle.util.DefaultTimer)
 *
 *   // In tests (for instant execution of delays)
 *   implicit val timer: Timer[Rerunnable] = RerunnableTimer(com.twitter.util.Timer.Nil)
 *
 *   // A dedicated `JavaTimer`
 *   implicit val timer: Timer[Rerunnable] = RerunnableTimer()
 * }}}
 */
object RerunnableTimer {

  def apply(implicit twitterTimer: util.Timer): RerunnableTimer = new RerunnableTimer

  def apply(): RerunnableTimer = {
    implicit val twitterTimer: util.Timer = new util.JavaTimer

    new RerunnableTimer
  }
}

final private[effect] class RerunnableTimer private (implicit underlyingTimer: util.Timer) extends Timer[Rerunnable] {

  override val clock: Clock[Rerunnable] = RerunnableClock()

  override def sleep(duration: FiniteDuration): Rerunnable[Unit] =
    Rerunnable.fromFuture(
      Future.Unit.delayed(util.Duration.fromNanoseconds(duration.toNanos))
    )
}
