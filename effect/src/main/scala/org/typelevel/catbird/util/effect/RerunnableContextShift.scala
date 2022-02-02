package org.typelevel.catbird.util.effect

import cats.effect.ContextShift
import com.twitter.util.{ Future, FuturePool, Promise }
import org.typelevel.catbird.util.Rerunnable

import scala.Unit
import java.lang.Runnable
import java.util.concurrent.ExecutorService

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutorService }

/**
 * The goal here is to provide an implicit instance for `ContextShift[Rerunnable]`, so you can use libraries like
 * `fs2` in a finagle-based application without converting between `Future` and `IO` everywhere.
 *
 * Usage:
 * {{{
 *   implicit val rerunnableCS: ContextShift[Rerunnable] = RerunnableContextShift.global
 * }}}
 */
object RerunnableContextShift {

  final def fromExecutionContext(ec: ExecutionContext): ContextShift[Rerunnable] =
    new RerunnableContextShift(ec)

  final def fromExecutorService(es: ExecutorService): ContextShift[Rerunnable] =
    fromExecutionContext(ExecutionContext.fromExecutorService(es))

  final def fromExecutionContextExecutorService(eces: ExecutionContextExecutorService): ContextShift[Rerunnable] =
    fromExecutorService(eces)

  final lazy val global: ContextShift[Rerunnable] =
    fromExecutionContext(scala.concurrent.ExecutionContext.global)

  /**
   * Mirrors the api of `scala.concurrent.ExecutionContext.Implicit.global`.
   *
   * Usage:
   * {{{
   *   import org.typelevel.catbird.util.effect.RerunnableContextShift.Implicits.global
   * }}}
   */
  object Implicits {
    final implicit def global: ContextShift[Rerunnable] = RerunnableContextShift.global
  }
}

final private[effect] class RerunnableContextShift private (ec: ExecutionContext) extends ContextShift[Rerunnable] {
  private final lazy val futurePool = FuturePool.interruptible(ec.asInstanceOf[ExecutionContextExecutorService])

  override def shift: Rerunnable[Unit] =
    Rerunnable.withFuturePool(futurePool)(()) // This is a bit of a hack, but it will have to do

  override def evalOn[A](targetEc: ExecutionContext)(fa: Rerunnable[A]): Rerunnable[A] =
    for {
      r <- executeOn(targetEc)(fa).liftToTry
      _ <- shift
      a <- Rerunnable.fromFuture(Future.value(r).lowerFromTry)
    } yield a

  private def executeOn[A](targetEc: ExecutionContext)(fa: Rerunnable[A]): Rerunnable[A] =
    Rerunnable.fromFuture {
      val p = Promise[A]()

      targetEc.execute(new Runnable {
        override def run(): Unit =
          fa.run.proxyTo[A](p)
      })

      p
    }
}
