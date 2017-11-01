package io.catbird.util

import cats.{ Comonad, Eq }
import cats.instances.either._
import cats.instances.int._
import cats.instances.tuple._
import cats.instances.unit._
import cats.kernel.laws.discipline.{ MonoidTests, SemigroupTests }
import cats.laws.discipline._
import cats.laws.discipline.arbitrary._
import com.twitter.conversions.time._
import com.twitter.util.Future
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline

class FutureSuite extends FunSuite with Discipline with FutureInstances with ArbitraryInstances with EqInstances {
  implicit val eqFutureInt: Eq[Future[Int]] = futureEqWithFailure(1.second)
  implicit val eqFutureFutureInt: Eq[Future[Future[Int]]] = futureEqWithFailure(1.second)
  implicit val eqFutureFutureFutureInt: Eq[Future[Future[Future[Int]]]] = futureEqWithFailure(1.second)
  implicit val eqFutureInt3: Eq[Future[(Int, Int, Int)]] = futureEqWithFailure(1.second)
  implicit val eqFutureEitherUnit: Eq[Future[Either[Throwable, Unit]]] = futureEqWithFailure(1.second)
  implicit val eqFutureEitherInt: Eq[Future[Either[Throwable, Int]]] = futureEqWithFailure(1.second)
  implicit val comonad: Comonad[Future] = futureComonad(1.second)

  checkAll("Future[Int]", MonadErrorTests[Future, Throwable].monadError[Int, Int, Int])
  checkAll("Future[Int]", ComonadTests[Future].comonad[Int, Int, Int])
  checkAll("Future[Int]", FunctorTests[Future](comonad).functor[Int, Int, Int])
  checkAll("Future[Int]", SemigroupTests[Future[Int]](twitterFutureSemigroup[Int]).semigroup)
  checkAll("Future[Int]", MonoidTests[Future[Int]].monoid)
}
