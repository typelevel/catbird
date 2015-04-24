package io.catbird.test.util

import cats.laws.discipline.ArbitraryK
import com.twitter.util.{ Future, Return, Try, Var }
import org.scalacheck.Arbitrary

trait ArbitraryKInstances extends ArbitraryInstances {
  implicit def futureArbitraryK: ArbitraryK[Future] =
    new ArbitraryK[Future] {
      def synthesize[A](implicit A: Arbitrary[A]): Arbitrary[Future[A]] = futureArbitrary
    }

  implicit def varArbitraryK: ArbitraryK[Var] =
    new ArbitraryK[Var] {
      def synthesize[A](implicit A: Arbitrary[A]): Arbitrary[Var[A]] = varArbitrary
    }
}

trait ArbitraryInstances {
  implicit def futureArbitrary[A](implicit A: Arbitrary[A]): Arbitrary[Future[A]] =
    Arbitrary(A.arbitrary.map(Future.value))

  implicit def tryArbitrary[A](implicit A: Arbitrary[A]): Arbitrary[Try[A]] =
    Arbitrary(A.arbitrary.map(Return(_)))

  implicit def varArbitrary[A](implicit A: Arbitrary[A]): Arbitrary[Var[A]] =
    Arbitrary(A.arbitrary.map(Var.value))
}
