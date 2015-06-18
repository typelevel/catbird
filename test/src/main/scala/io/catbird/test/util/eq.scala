package io.catbird.test.util

import cats.Eq
import cats.laws.discipline.EqK
import com.twitter.util.{ Duration, Future, Var }
import io.catbird.util.{ futureEq, varEq }

trait EqKInstances {
  def futureEqK(atMost: Duration): EqK[Future] =
    new EqK[Future] {
      def synthesize[A](implicit A: Eq[A]): Eq[Future[A]] = futureEq(atMost)
    }

  implicit def varEqK: EqK[Var] =
    new EqK[Var] {
      def synthesize[A](implicit A: Eq[A]): Eq[Var[A]] = varEq
    }
}
