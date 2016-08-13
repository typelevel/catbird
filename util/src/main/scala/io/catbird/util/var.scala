package io.catbird.util

import cats.{ CoflatMap, Comonad, Eq, Monad, Monoid, Semigroup }
import cats.data.Xor
import com.twitter.util.Var
import scala.Boolean

trait VarInstances extends VarInstances1 {
  implicit final val twitterVarInstance: Monad[Var] with CoflatMap[Var] =
    new VarCoflatMap with Monad[Var] {
      final def pure[A](x: A): Var[A] = Var.value(x)
      final def flatMap[A, B](fa: Var[A])(f: A => Var[B]): Var[B] = fa.flatMap(f)
      override final def map[A, B](fa: Var[A])(f: A => B): Var[B] = fa.map(f)
    }

  implicit final def twitterVarSemigroup[A](implicit A: Semigroup[A]): Semigroup[Var[A]] =
    new VarSemigroup[A]

  final def varEq[A](implicit A: Eq[A]): Eq[Var[A]] =
    new Eq[Var[A]] {
      final def eqv(fx: Var[A], fy: Var[A]): Boolean = Var.sample(
        fx.join(fy).map {
          case (x, y) => A.eqv(x, y)
        }
      )
    }
}

trait VarInstances1 {
  final def varComonad: Comonad[Var] = new VarCoflatMap with Comonad[Var] {
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

  final def tailRecM[A, B](a: A)(f: A => Var[Xor[A, B]]): Var[B] =
    f(a).flatMap {
      case Xor.Left(a1) => tailRecM(a1)(f)
      case Xor.Right(b) => Var.value(b)
    }
}

private[util] class VarSemigroup[A](implicit A: Semigroup[A]) extends Semigroup[Var[A]] {
  final def combine(fx: Var[A], fy: Var[A]): Var[A] = fx.join(fy).map {
    case (x, y) => A.combine(x, y)
  }
}
