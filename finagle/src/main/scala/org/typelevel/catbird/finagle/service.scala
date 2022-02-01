package org.typelevel.catbird.finagle

import cats.arrow.{ Category, Profunctor }
import org.typelevel.catbird.util.twitterFutureInstance
import com.twitter.finagle.Service

trait ServiceInstances {
  implicit val serviceInstance: Category[Service] with Profunctor[Service] =
    new Category[Service] with Profunctor[Service] {
      final def id[A]: Service[A, A] = Service.mk(twitterFutureInstance.pure)

      final def compose[A, B, C](f: Service[B, C], g: Service[A, B]): Service[A, C] =
        Service.mk(a => g(a).flatMap(f))

      final def dimap[A, B, C, D](fab: Service[A, B])(f: C => A)(g: B => D): Service[C, D] =
        Service.mk(c => fab.map(f)(c).map(g))

      override final def lmap[A, B, C](fab: Service[A, B])(f: C => A): Service[C, B] = fab.map(f)
    }
}
