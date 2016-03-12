package io.catbird.tests

import algebra.Eq

trait EqInstances {
  implicit def throwableEq: Eq[Throwable] = Eq.fromUniversalEquals
}
