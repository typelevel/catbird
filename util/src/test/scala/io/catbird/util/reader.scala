package io.catbird
package util

import cats.Eq
import cats.instances.boolean._
import cats.instances.int._
import cats.instances.tuple._
import cats.kernel.laws.discipline.MonoidTests
import cats.laws.discipline.{ AlternativeTests, DeferTests, MonadTests }
import com.twitter.io.Reader
import com.twitter.conversions.DurationOps._

class ReaderSuite extends CatbirdSuite with ReaderInstances with ArbitraryInstances with EqInstances {
  implicit private def eqReader[A: Eq]: Eq[Reader[A]] = readerEq[A](1.second)

  checkAll("Reader[Int]", AlternativeTests[Reader].alternative[Int, Int, Int])
  checkAll("Reader[Int]", DeferTests[Reader].defer[Int])
  checkAll("Reader[Int]", MonadTests[Reader].monad[Int, Int, Int])

  checkAll("Reader[Int]", MonoidTests[Reader[Int]].monoid)
}
