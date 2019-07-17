package io.catbird.util.internal

/** INTERNAL API — Newtype encoding for types with one type parameter.
 *
 * The `Newtype1` abstract class indirection is needed for Scala 2.10,
 * otherwise we could just define these types straight on the
 * companion object. In Scala 2.10 definining these types
 * straight on the companion object yields an error like:
 * ''"only classes can have declared but undefined members"''.
 *
 * Inspired by
 * [[https://github.com/alexknvl/newtypes alexknvl/newtypes]].
 */
private[util] abstract class Newtype1[F[_]] { self =>
  type Base
  trait Tag extends scala.Any
  type Type[+A] <: Base with Tag

  def apply[A](fa: F[A]): Type[A] =
    fa.asInstanceOf[Type[A]]

  def unwrap[A](fa: Type[A]): F[A] =
    fa.asInstanceOf[F[A]]
}
