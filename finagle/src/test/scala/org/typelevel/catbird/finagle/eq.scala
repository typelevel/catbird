package org.typelevel.catbird.finagle

import cats.kernel.Eq
import com.twitter.finagle.Service
import com.twitter.util.{ Await, Duration }

trait EqInstances {
  def serviceEq[I, O](atMost: Duration)(implicit eqF: Eq[I => O]): Eq[Service[I, O]] =
    Eq.by[Service[I, O], I => O](_.andThen(o => Await.result(o, atMost)))(eqF)
}
