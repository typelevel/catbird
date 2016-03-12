package io.catbird.util

import cats.{ Eq, MonadError, Monoid, Semigroup }
import com.twitter.util.{ Return, Throw, Try }

trait TryInstances extends TryInstances1 {
  implicit final def tryEq[A](implicit A: Eq[A]): Eq[Try[A]] =
    new Eq[Try[A]] {
      def eqv(x: Try[A], y: Try[A]): Boolean = (x, y) match {
        case (Throw(xError), Throw(yError)) => xError == yError
        case (Return(xValue), Return(yValue)) => A.eqv(xValue, yValue)
        case _ => false
      }
    }

  implicit final def trySemigroup[A](implicit A: Semigroup[A]): Semigroup[Try[A]] =
    new TrySemigroup[A]

  implicit final val tryInstance: MonadError[Try, Throwable] = new MonadError[Try, Throwable] {
    final def pure[A](x: A): Try[A] = Return(x)
      final def flatMap[A, B](fa: Try[A])(f: A => Try[B]): Try[B] = fa.flatMap(f)
      override final def map[A, B](fa: Try[A])(f: A => B): Try[B] = fa.map(f)

      final def handleErrorWith[A](fa: Try[A])(f: Throwable => Try[A]): Try[A] =
        fa.rescue {
          case e => f(e)
        }
      final def raiseError[A](e: Throwable): Try[A] = Throw(e)
  }
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
