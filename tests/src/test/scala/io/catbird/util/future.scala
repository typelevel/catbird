package io.catbird.util

import algebra.laws.GroupLaws
import cats.{ Comonad, Eq }
import cats.data.Xor
import cats.laws.discipline._
import cats.laws.discipline.eq._
import cats.std.int._
import com.twitter.conversions.time._
import com.twitter.util.Future
import io.catbird.tests.EqInstances
import io.catbird.tests.util.ArbitraryInstances
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline

class FutureSuite extends FunSuite with Discipline with FutureInstances with ArbitraryInstances with EqInstances {
  implicit val eqFutureInt: Eq[Future[Int]] = futureEqWithFailure(1.second)
  implicit val eqFutureFutureInt: Eq[Future[Future[Int]]] = futureEqWithFailure(1.second)
  implicit val eqFutureFutureFutureInt: Eq[Future[Future[Future[Int]]]] = futureEqWithFailure(1.second)
  implicit val eqFutureInt3: Eq[Future[(Int, Int, Int)]] = futureEqWithFailure(1.second)
  implicit val eqFutureXorUnit: Eq[Future[Xor[Throwable, Unit]]] = futureEqWithFailure(1.second)
  implicit val eqFutureXorInt: Eq[Future[Xor[Throwable, Int]]] = futureEqWithFailure(1.second)
  implicit val comonad: Comonad[Future] = futureComonad(1.second)

  checkAll("Future[Int]", MonadErrorTests[Future, Throwable].monadError[Int, Int, Int])
  checkAll("Future[Int]", ComonadTests[Future].comonad[Int, Int, Int])
  checkAll("Future[Int]", FunctorTests[Future](comonad).functor[Int, Int, Int])
  checkAll("Future[Int]", GroupLaws[Future[Int]].semigroup(futureSemigroup[Int]))
  checkAll("Future[Int]", GroupLaws[Future[Int]].monoid)
}
