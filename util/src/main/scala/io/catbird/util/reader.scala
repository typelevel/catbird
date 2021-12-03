package io.catbird
package util

import cats.{ Alternative, Defer, Eq, Monad, Monoid, MonoidK, StackSafeMonad }
import cats.instances.list._
import com.twitter.io.Reader
import com.twitter.util.{ Await, Duration }
import scala.collection.immutable.List

/** Typeclass instances for [[com.twitter.io.Reader]]
  *
  * Note that while (to the best of my knowledge) these instances are lawful, Reader itself is inherently
  * unsafe: it is based on mutable state and also requires manual cleanup to ensure resource safety.  To use
  * it in pure functional code, side-effecting operations should be wrapped in an effect type such as
  * [[Rerunnable]], and cleanup should be ensured using something like [[cats.effect.Resource]].
  *
  * TODO: add facilities to make that easier to do.
  */
trait ReaderInstances {
  implicit final val readerInstance: Alternative[Reader] with Defer[Reader] with Monad[Reader] =
    new Alternative[Reader] with ReaderDefer with ReaderMonad with ReaderMonoidK

  implicit final def readerMonoid[A](implicit A: Monoid[A]) = new ReaderMonoid[A]

  /**
   * Obtain a [[cats.Eq]] instance for [[com.twitter.io.Reader]].
   *
   * These instances use [[com.twitter.util.Await]] so should be
   * [[https://finagle.github.io/blog/2016/09/01/block-party/ avoided in production code]].  Likely use cases
   * include tests, scrips, REPLs etc.
   */
  final def readerEq[A](atMost: Duration)(implicit A: Eq[A]): Eq[Reader[A]] = new Eq[Reader[A]] {
    final override def eqv(x: Reader[A], y: Reader[A]) =
      Await.result(
        Reader.readAllItems(x).joinWith(Reader.readAllItems(y))((xs, ys) => Eq[List[A]].eqv(xs.toList, ys.toList)),
        atMost
      )
  }
}

/** Monad instance for twitter-util's Reader streaming abstraction
  *
  * Also entrant for "most confusing class name of the year"
  */
private[util] trait ReaderMonad extends StackSafeMonad[Reader] {
  final override def map[A, B](fa: Reader[A])(f: A => B): Reader[B] = fa.map(f)
  final override def pure[A](a: A): Reader[A] = Reader.value(a)
  final override def flatMap[A, B](fa: Reader[A])(f: A => Reader[B]): Reader[B] = fa.flatMap(f)
  final override def flatten[A](ffa: Reader[Reader[A]]): Reader[A] = ffa.flatten
}

private[util] trait ReaderDefer extends Defer[Reader] {
  /** Defer creation of a Reader
    *
    * There are a few ways to achieve this, such as using fromIterator on a lazy iterator, but this is the
    * simplest I've come up with.  Might be worth benchmarking alternatives.
    */
  def defer[A](fa: => Reader[A]): Reader[A] = Reader.flatten(Reader.value(() => fa).map(_()))
}

private[util] trait ReaderMonoidK extends MonoidK[Reader] {
  final override def empty[A]: Reader[A] = Reader.empty
  final override def combineK[A](x: Reader[A], y: Reader[A]): Reader[A] = Reader.concat(List(x, y))
}

private[util] final class ReaderMonoid[A](implicit A: Monoid[A]) extends Monoid[Reader[A]] {
  final override def empty = Reader.value(A.empty)
  final override def combine(xs: Reader[A], ys: Reader[A]) =
    for(x <- xs; y <- ys) yield A.combine(x, y)
}
