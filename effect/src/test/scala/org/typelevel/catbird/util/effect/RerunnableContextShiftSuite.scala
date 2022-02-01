package org.typelevel.catbird.util.effect

import cats.effect.{ ContextShift, IO, Sync }
import com.twitter.util.{ Await, Future, FuturePool }
import org.typelevel.catbird.util.Rerunnable
import org.scalatest.Outcome
import org.scalatest.funsuite.FixtureAnyFunSuite

class RerunnableContextShiftSuite extends FixtureAnyFunSuite with ThreadPoolNamingSupport {

  protected final class FixtureParam {
    val futurePoolName = "future-pool"
    val otherPoolName = "other-pool"
    val ioPoolName = "io-pool"

    val futurePool = FuturePool.interruptible(newNamedThreadPool(futurePoolName))
    val otherPool = newNamedThreadPool(otherPoolName)
    val ioPool = newNamedThreadPool(ioPoolName)

    def newIO: IO[String] = IO(currentThreadName())

    def newFuture: Future[String] = futurePool(currentThreadName())

    def newRerunnable: Rerunnable[String] = Rerunnable(currentThreadName())
  }

  test("ContextShift[Rerunnable].shift shifts to the pool of the instance") { f =>
    implicit val cs: ContextShift[Rerunnable] =
      RerunnableContextShift.fromExecutionContext(f.ioPool)

    val (poolName1, poolName2, poolName3) =
      (for {
        poolName1 <- Rerunnable.fromFuture(f.newFuture)

        _ <- ContextShift[Rerunnable](cs).shift

        poolName2 <- Sync[Rerunnable].delay(currentThreadName())

        poolName3 <- Rerunnable.fromFuture(f.newFuture)
      } yield (poolName1, poolName2, poolName3)).run.await

    assert(poolName1 == f.futurePoolName)
    assert(poolName2 == f.ioPoolName)
    assert(poolName2 == f.ioPoolName)
    assert(poolName3 == f.futurePoolName)
  }

  test("ContextShift[Rerunnable].evalOn executes on correct pool and shifts back to previous pool") { f =>
    implicit val cs: ContextShift[Rerunnable] =
      RerunnableContextShift.fromExecutionContext(f.ioPool)

    val (poolName1, poolName2, poolName3) =
      (for {
        poolName1 <- f.newRerunnable

        poolName2 <- ContextShift[Rerunnable].evalOn(f.otherPool)(f.newRerunnable)

        poolName3 <- f.newRerunnable
      } yield (poolName1, poolName2, poolName3)).run.await

    assert(poolName1 == currentThreadName()) // The first rerunnable is not explicitly evaluated on a dedicated pool
    assert(poolName2 == f.otherPoolName)
    assert(poolName3 == f.ioPoolName)
  }

  test("ContextShift[Rerunnable].evalOn executes on correct pool and shifts back to future pool") { f =>
    implicit val cs: ContextShift[Rerunnable] =
      RerunnableContextShift.fromExecutionContext(f.ioPool)

    val (poolName1, poolName2, poolName3) =
      (for {
        poolName1 <- Rerunnable.fromFuture(f.newFuture) // The future was started on a dedicated pool (e.g. netty)

        poolName2 <- ContextShift[Rerunnable].evalOn(f.otherPool)(f.newRerunnable)

        poolName3 <- Rerunnable.fromFuture(f.newFuture)
      } yield (poolName1, poolName2, poolName3)).run.await

    assert(poolName1 == f.futurePoolName)
    assert(poolName2 == f.otherPoolName)
    assert(poolName3 == f.futurePoolName)
  }

  implicit private class FutureAwaitOps[A](future: Future[A]) {
    def await: A = Await.result(future)
  }

  override protected def withFixture(test: OneArgTest): Outcome = withFixture(test.toNoArgTest(new FixtureParam))
}
