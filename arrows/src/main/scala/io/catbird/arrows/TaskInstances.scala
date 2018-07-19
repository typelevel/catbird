package io.catbird.arrows

import _root_.arrows.twitter.Task
import cats.{ CoflatMap, Comonad, Eq, MonadError, Monoid, Semigroup }
import com.twitter.util.{ Await, Duration, Return, Throw, Try }
import java.lang.Throwable
import scala.Boolean
import scala.util.{ Either, Left, Right }

trait TaskInstances extends TaskInstances1 {
  implicit final val arrowsTaskInstance: MonadError[Task, Throwable] with CoflatMap[Task] =
    new TaskCoflatMap with MonadError[Task, Throwable] {
      final def pure[A](x: A): Task[A] = Task.value(x)
      final def flatMap[A, B](fa: Task[A])(f: A => Task[B]): Task[B] = fa.flatMap(f)
      override final def map[A, B](fa: Task[A])(f: A => B): Task[B] = fa.map(f)
      override final def ap[A, B](f: Task[A => B])(fa: Task[A]): Task[B] = Task.join(f, fa).map {
        case (ab, a) => ab(a)
      }
      override final def product[A, B](fa: Task[A], fb: Task[B]): Task[(A, B)] = Task.join(fa, fb)

      final def handleErrorWith[A](fa: Task[A])(f: Throwable => Task[A]): Task[A] =
        fa.rescue {
          case e => f(e)
        }
      final def raiseError[A](e: Throwable): Task[A] = Task.exception(e)

      final def tailRecM[A, B](a: A)(f: A => Task[Either[A, B]]): Task[B] = f(a).flatMap {
        case Right(b) => pure(b)
        case Left(nextA) => tailRecM(nextA)(f)
      }
    }

  implicit final def arrowsTaskSemigroup[A](implicit A: Semigroup[A]): Semigroup[Task[A]] =
    new TaskSemigroup[A]

  final def taskEq[A](atMost: Duration)(implicit A: Eq[A]): Eq[Task[A]] = new Eq[Task[A]] {
    final def eqv(x: Task[A], y: Task[A]): Boolean = Await.result(
      Task.join(x, y).map {
        case (xa, ya) => A.eqv(xa, ya)
      }.run(()),
      atMost
    )
  }

  final def taskEqWithFailure[A](atMost: Duration)(implicit A: Eq[A], T: Eq[Throwable]): Eq[Task[A]] = {
    val tryEq = new Eq[Try[A]] {
      def eqv(x: Try[A], y: Try[A]): Boolean = (x, y) match {
        case (Throw(xError), Throw(yError)) => T.eqv(xError, yError)
        case (Return(xValue), Return(yValue)) => A.eqv(xValue, yValue)
        case _ => false
      }
    }

    Eq.by[Task[A], Task[Try[A]]](_.liftToTry)(taskEq[Try[A]](atMost)(tryEq))
  }
}

private[arrows] trait TaskInstances1 {
  final def taskComonad(atMost: Duration): Comonad[Task] =
    new TaskCoflatMap with Comonad[Task] {
      final def extract[A](x: Task[A]): A = Await.result(x.run(()), atMost)
      final def map[A, B](fa: Task[A])(f: A => B): Task[B] = fa.map(f)
    }

  implicit final def arrowsTaskMonoid[A](implicit A: Monoid[A]): Monoid[Task[A]] =
    new TaskSemigroup[A] with Monoid[Task[A]] {
      final def empty: Task[A] = Task.value(A.empty)
    }
}

private[arrows] sealed abstract class TaskCoflatMap extends CoflatMap[Task] {
  final def coflatMap[A, B](fa: Task[A])(f: Task[A] => B): Task[B] = Task(f(fa))
}

private[arrows] sealed class TaskSemigroup[A](implicit A: Semigroup[A])
  extends Semigroup[Task[A]] {
    final def combine(fx: Task[A], fy: Task[A]): Task[A] = Task.join(fx, fy).map {
      case (x, y) => A.combine(x, y)
    }
  }
