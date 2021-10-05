package org.typelevel.catbird.finagle

import com.twitter.finagle.Service
import org.typelevel.catbird.util.twitterFutureInstance
import org.scalacheck.Arbitrary

trait ArbitraryInstances {
  implicit def serviceArbitrary[I, O](implicit arbF: Arbitrary[I => O]): Arbitrary[Service[I, O]] =
    Arbitrary(arbF.arbitrary.map(f => Service.mk(i => twitterFutureInstance.pure(f(i)))))
}
