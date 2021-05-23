package io.catbird.util.effect

import cats.Eq
import cats.effect.laws.MonadCancelTests
import cats.instances.all._
import cats.laws.discipline.arbitrary._
import com.twitter.conversions.DurationOps._
import com.twitter.util.Future
import io.catbird.util.futureEqWithFailure

class FutureSuite extends BaseLawSuite {

  implicit def futureEq[A](implicit A: Eq[A]): Eq[Future[A]] =
    futureEqWithFailure(1.seconds)

  checkAll("Future[Int]", MonadCancelTests[Future, Throwable].monadCancel[Int, Int, Int])
}
