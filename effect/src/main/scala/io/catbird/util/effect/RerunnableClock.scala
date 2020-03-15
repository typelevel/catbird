package io.catbird.util.effect

import java.util.concurrent.TimeUnit

import cats.effect.Clock
import io.catbird.util.Rerunnable
import scala.Long
import java.lang.System

import scala.concurrent.duration.TimeUnit

object RerunnableClock {

  def apply(): RerunnableClock = new RerunnableClock
}

final private[effect] class RerunnableClock extends Clock[Rerunnable] {

  override def realTime(unit: TimeUnit): Rerunnable[Long] =
    Rerunnable(unit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS))

  override def monotonic(unit: TimeUnit): Rerunnable[Long] =
    Rerunnable(unit.convert(System.nanoTime(), TimeUnit.NANOSECONDS))
}
