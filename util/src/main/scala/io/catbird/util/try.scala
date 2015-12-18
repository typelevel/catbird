package io.catbird.util

import cats.{ Eq, Monoid, Semigroup }
import cats.data.Xor
import com.twitter.bijection.Bijection
import com.twitter.util.{ Return, Throw, Try }

trait TryInstances extends TryInstances1 {
  /**
   * Here for the sake of convenience, but needs a better home.
   */
  implicit def throwableEq: Eq[Throwable] = new Eq[Throwable] {
    def eqv(x: Throwable, y: Throwable): Boolean = x == y
  }

  implicit def tryEq[A](implicit A: Eq[A]): Eq[Try[A]] =
    new Eq[Try[A]] {
      def eqv(x: Try[A], y: Try[A]): Boolean = (x, y) match {
        case (Throw(xError), Throw(yError)) => throwableEq.eqv(xError, yError)
        case (Return(xValue), Return(yValue)) => A.eqv(xValue, yValue)
        case _ => false
      }
    }

  implicit def trySemigroup[A](implicit A: Semigroup[A]): Semigroup[Try[A]] = new TrySemigroup[A]
}

private[util] trait TryInstances1 {
  implicit def tryMonoid[A](implicit A: Monoid[A]): Monoid[Try[A]] =
    new TrySemigroup[A] with Monoid[Try[A]] {
      def empty: Try[A] = Return(A.empty)
    }
}

trait TryConversions {
  implicit def tryToXor[A]: Bijection[Try[A], Xor[Throwable, A]] =
    Bijection.build[Try[A], Xor[Throwable, A]] {
      case Throw(error) => Xor.left(error)
      case Return(value) => Xor.right(value)
    } { _.fold(Throw(_), Return(_)) }
}

private[util] class TrySemigroup[A](implicit A: Semigroup[A]) extends Semigroup[Try[A]] {
  def combine(fx: Try[A], fy: Try[A]): Try[A] = for {
    vx <- fx
    vy <- fy
  } yield A.combine(vx, vy)
}
