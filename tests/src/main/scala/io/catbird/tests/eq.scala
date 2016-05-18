package io.catbird.tests

import cats.Eq

trait EqInstances {
  implicit def throwableEq: Eq[Throwable] = Eq.fromUniversalEquals
}
