package io.catbird.util

import cats.{ Eq, Monoid, Semigroup }
import com.twitter.util.{ Return, Throw, Try }

trait TryInstances extends TryInstances1 {
  /**
   * Here for the sake of convenience, but needs a better home.
   */
  implicit final def throwableEq: Eq[Throwable] = Eq.fromUniversalEquals

  implicit final def tryEq[A](implicit A: Eq[A]): Eq[Try[A]] =
    new Eq[Try[A]] {
      def eqv(x: Try[A], y: Try[A]): Boolean = (x, y) match {
        case (Throw(xError), Throw(yError)) => throwableEq.eqv(xError, yError)
        case (Return(xValue), Return(yValue)) => A.eqv(xValue, yValue)
        case _ => false
      }
    }

  implicit final def trySemigroup[A](implicit A: Semigroup[A]): Semigroup[Try[A]] =
    new TrySemigroup[A]
}

private[util] trait TryInstances1 {
  implicit final def tryMonoid[A](implicit A: Monoid[A]): Monoid[Try[A]] =
    new TrySemigroup[A] with Monoid[Try[A]] {
      final def empty: Try[A] = Return(A.empty)
    }
}

private[util] class TrySemigroup[A](implicit A: Semigroup[A]) extends Semigroup[Try[A]] {
  final def combine(fx: Try[A], fy: Try[A]): Try[A] = for {
    vx <- fx
    vy <- fy
  } yield A.combine(vx, vy)
}
