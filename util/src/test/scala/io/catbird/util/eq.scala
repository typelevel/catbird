package io.catbird.util

import cats.Eq

trait EqInstances {
  implicit def throwableEq: Eq[Throwable] = Eq.fromUniversalEquals
}
