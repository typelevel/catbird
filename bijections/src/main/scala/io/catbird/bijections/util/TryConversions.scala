package io.catbird.bijections.util

import algebra.Semigroup
import cats.data.Xor
import com.twitter.bijection.Bijection
import com.twitter.util.{ Return, Throw, Try }

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
