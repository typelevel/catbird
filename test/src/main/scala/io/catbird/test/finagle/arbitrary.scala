package io.catbird.test.finagle

import com.twitter.finagle.Service
import io.catbird.util.futureInstance
import org.scalacheck.Arbitrary

trait ArbitraryInstances {
  implicit def serviceArbitrary[I, O](implicit arbF: Arbitrary[I => O]): Arbitrary[Service[I, O]] =
    Arbitrary(arbF.arbitrary.map(f => Service.mk(i => futureInstance.pure(f(i)))))
}
