package io.catbird.tests.finagle

import algebra.Eq
import com.twitter.finagle.Service
import com.twitter.util.{ Await, Duration }

trait EqInstances {
  def serviceEq[I, O](atMost: Duration)(implicit eqF: Eq[I => O]): Eq[Service[I, O]] =
    eqF.on(_.andThen(o => Await.result(o, atMost)))
}
