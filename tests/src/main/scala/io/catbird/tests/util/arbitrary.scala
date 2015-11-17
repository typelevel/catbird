package io.catbird.tests.util

import com.twitter.util.{ Future, Return, Try, Var }
import org.scalacheck.Arbitrary

trait ArbitraryInstances {
  implicit def futureArbitrary[A](implicit A: Arbitrary[A]): Arbitrary[Future[A]] =
    Arbitrary(A.arbitrary.map(Future.value))

  implicit def tryArbitrary[A](implicit A: Arbitrary[A]): Arbitrary[Try[A]] =
    Arbitrary(A.arbitrary.map(Return(_)))

  implicit def varArbitrary[A](implicit A: Arbitrary[A]): Arbitrary[Var[A]] =
    Arbitrary(A.arbitrary.map(Var.value))
}
