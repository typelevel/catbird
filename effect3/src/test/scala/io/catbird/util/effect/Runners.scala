package io.catbird.util.effect

import cats.Eq
import cats.effect.kernel.Outcome
import cats.effect.kernel.testkit.SyncGenerators
import com.twitter.util.{ Await, Return, Throw }
import io.catbird.util.{ EqInstances, Rerunnable }
import org.scalacheck.{ Arbitrary, Cogen, Prop }

import scala.language.implicitConversions

trait Runners { self: EqInstances =>

  implicit def arbitraryRerunnable[A: Arbitrary: Cogen]: Arbitrary[Rerunnable[A]] = {
    val generators = new SyncGenerators[Rerunnable] {
      val F = rerunnableInstance

      val arbitraryE = Arbitrary.arbThrowable
      val cogenE = Cogen.cogenThrowable

      val arbitraryFD = Arbitrary.arbFiniteDuration
    }
    Arbitrary(generators.generators[A])
  }

  implicit def eqRerunnableA[A: Eq]: Eq[Rerunnable[A]] =
    Eq.by(unsafeRun(_))

  implicit def boolRunnings(rerunnableB: Rerunnable[Boolean]): Prop =
    Prop(unsafeRun(rerunnableB).fold(false, _ => false, _.getOrElse(false)))

  def unsafeRun[A](fa: Rerunnable[A]): Outcome[Option, Throwable, A] =
    Await.result(
      fa.liftToTry
        .map[Outcome[Option, Throwable, A]] {
          case Return(a) => Outcome.succeeded(Some(a))
          case Throw(e)  => Outcome.errored(e)
        }
        .run
    )
}
