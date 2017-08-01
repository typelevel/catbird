package io.catbird.util

import cats.kernel.Eq

trait EqInstances {
  implicit def throwableEq: Eq[Throwable] = Eq.fromUniversalEquals
}
