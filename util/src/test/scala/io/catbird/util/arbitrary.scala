package io.catbird.util

import com.twitter.concurrent.AsyncStream
import com.twitter.conversions.DurationOps._
import com.twitter.util.{ Future, Return, Try, Var }
import org.scalacheck.{ Arbitrary, Cogen }

trait ArbitraryInstances {
  implicit def futureArbitrary[A](implicit A: Arbitrary[A]): Arbitrary[Future[A]] =
    Arbitrary(A.arbitrary.map(Future.value))

  implicit def tryArbitrary[A](implicit A: Arbitrary[A]): Arbitrary[Try[A]] =
    Arbitrary(A.arbitrary.map(Return(_)))

  implicit def varArbitrary[A](implicit A: Arbitrary[A]): Arbitrary[Var[A]] =
    Arbitrary(A.arbitrary.map(Var.value))

  implicit def asyncStreamArbitrary[A](implicit A: Arbitrary[A]): Arbitrary[AsyncStream[A]] =
    Arbitrary(A.arbitrary.map(AsyncStream.of))

  implicit def rerunnableArbitrary[A](implicit A: Arbitrary[A]): Arbitrary[Rerunnable[A]] =
    Arbitrary(futureArbitrary[A].arbitrary.map(Rerunnable.fromFuture[A](_)))

  implicit def cogenFuture[A](implicit A: Cogen[A]): Cogen[Future[A]] =
    A.contramap(futureComonad(1.second).extract)

  implicit def cogenVar[A](implicit A: Cogen[A]): Cogen[Var[A]] =
    A.contramap(varComonad.extract)

  implicit def cogenRerunnable[A](implicit A: Cogen[A]): Cogen[Rerunnable[A]] =
    A.contramap(Rerunnable.rerunnableComonad(1.second).extract)
}
