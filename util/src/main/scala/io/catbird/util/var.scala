package io.catbird.util

import cats.{ CoflatMap, Comonad, Eq, Monad, Monoid, Semigroup }
import com.twitter.util.Var
import scala.Boolean

trait VarInstances extends VarInstances1 {
  implicit final val twitterVarInstance: Monad[Var] with CoflatMap[Var] =
    new VarCoflatMap with Monad[Var] {
      def pure[A](x: A): Var[A] = Var.value(x)
      def flatMap[A, B](fa: Var[A])(f: A => Var[B]): Var[B] = fa.flatMap(f)
      override def map[A, B](fa: Var[A])(f: A => B): Var[B] = fa.map(f)
    }

  implicit final def twitterVarSemigroup[A](implicit A: Semigroup[A]): Semigroup[Var[A]] =
    new VarSemigroup[A]

  final def varEq[A](implicit A: Eq[A]): Eq[Var[A]] =
    new Eq[Var[A]] {
      def eqv(x: Var[A], y: Var[A]): Boolean = Var.sample((x join y).map((A.eqv _).tupled))
    }
}

trait VarInstances1 {
  final def varComonad: Comonad[Var] =
    new VarCoflatMap with Comonad[Var] {
      final def extract[A](x: Var[A]): A = Var.sample(x)
      final def map[A, B](fa: Var[A])(f: A => B): Var[B] = fa.map(f)
    }

  implicit final def twitterVarMonoid[A](implicit A: Monoid[A]): Monoid[Var[A]] =
    new VarSemigroup[A] with Monoid[Var[A]] {
      final def empty: Var[A] = Var.value(A.empty)
    }
}

private[util] abstract class VarCoflatMap extends CoflatMap[Var] {
  final def coflatMap[A, B](fa: Var[A])(f: Var[A] => B): Var[B] = Var(f(fa))
}

private[util] class VarSemigroup[A](implicit A: Semigroup[A]) extends Semigroup[Var[A]] {
  final def combine(fx: Var[A], fy: Var[A]): Var[A] = (fx join fy).map((A.combine _).tupled)
}
