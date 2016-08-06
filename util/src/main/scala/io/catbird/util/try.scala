package io.catbird.util

import cats.{ Applicative, CoflatMap, Eq, Eval, MonadError, MonadRec, Monoid, Semigroup, Traverse }
import cats.data.Xor
import com.twitter.util.{ Return, Throw, Try }
import java.lang.Throwable
import scala.{ Boolean, inline }
import scala.annotation.tailrec

trait TryInstances extends TryInstances1 {
  implicit final def twitterTryEq[A](implicit A: Eq[A], T: Eq[Throwable]): Eq[Try[A]] =
    new Eq[Try[A]] {
      def eqv(x: Try[A], y: Try[A]): Boolean = (x, y) match {
        case (Throw(xError), Throw(yError)) => T.eqv(xError, yError)
        case (Return(xValue), Return(yValue)) => A.eqv(xValue, yValue)
        case _ => false
      }
    }

  implicit final def twitterTrySemigroup[A](implicit A: Semigroup[A]): Semigroup[Try[A]] =
    new TrySemigroup[A]

  implicit final val twitterTryInstance: MonadError[Try, Throwable]
      with CoflatMap[Try] with Traverse[Try] with MonadRec[Try] =
    new MonadError[Try, Throwable] with CoflatMap[Try] with Traverse[Try] with MonadRec[Try] {
      final def pure[A](x: A): Try[A] = Return(x)
      final def flatMap[A, B](fa: Try[A])(f: A => Try[B]): Try[B] = fa.flatMap(f)
      override final def map[A, B](fa: Try[A])(f: A => B): Try[B] = fa.map(f)

      final def handleErrorWith[A](fa: Try[A])(f: Throwable => Try[A]): Try[A] = fa.rescue {
        case e => f(e)
      }
      final def raiseError[A](e: Throwable): Try[A] = Throw(e)

      final def coflatMap[A, B](ta: Try[A])(f: Try[A] => B): Try[B] = Try(f(ta))

      final def foldLeft[A, B](fa: Try[A], b: B)(f: (B, A) => B): B = fa match {
        case Return(a) => f(b, a)
        case Throw(_) => b
      }

      final def foldRight[A, B](fa: Try[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] = fa match {
        case Return(a) => f(a, lb)
        case Throw(_) => lb
      }

      final def traverse[G[_], A, B](fa: Try[A])(f: A => G[B])(implicit G: Applicative[G]): G[Try[B]] = fa match {
        case Return(a) => G.map(f(a))(Return(_))
        case t: Throw[_] => G.pure(TryInstances.castThrow[B](t))
      }

      @tailrec final def tailRecM[A, B](a: A)(f: A => Try[Xor[A, B]]): Try[B] = f(a) match {
        case t: Throw[_] => TryInstances.castThrow[B](t)
        case Return(Xor.Left(a1)) => tailRecM(a1)(f)
        case Return(Xor.Right(b)) => Return(b)
      }
    }
}

private[util] final object TryInstances {
  @inline final def castThrow[A](t: Throw[_]): Try[A] = t.asInstanceOf[Try[A]]
}

private[util] trait TryInstances1 {
  implicit final def twitterTryMonoid[A](implicit A: Monoid[A]): Monoid[Try[A]] =
    new TrySemigroup[A] with Monoid[Try[A]] {
      final def empty: Try[A] = Return(A.empty)
    }
}

private[util] class TrySemigroup[A](implicit A: Semigroup[A]) extends Semigroup[Try[A]] {
  final def combine(fx: Try[A], fy: Try[A]): Try[A] = fx.flatMap(x => fy.map(y => A.combine(x, y)))
}
