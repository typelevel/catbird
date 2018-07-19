package io.catbird.benchmark

import _root_.arrows.twitter.Task
import com.twitter.util.{ Await, Future, FuturePool }
import io.catbird.util.Rerunnable
import java.util.concurrent.{ ExecutorService, Executors, TimeUnit }
import org.openjdk.jmh.annotations._

/**
 * Compare the performance of `Rerunnable` against ordinary `Future`s.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/jmh:run -i 20 -wi 10 -f 2 -t 1 io.catbird.benchmark.RerunnableBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class RerunnableBenchmark {
  val count: Int = 100000
  val numbers: IndexedSeq[Int] = 0 to count
  var es: ExecutorService = _
  var pool: FuturePool = _

  @Setup
  def initPool(): Unit = {
    es = Executors.newFixedThreadPool(4)
    pool = FuturePool(es)
  }

  @TearDown
  def shutdownPool(): Unit = es.shutdown()

  @Benchmark
  def sumIntsF: Int = Await.result(
    numbers.foldLeft(Future(0)) {
      case (acc, i) => acc.flatMap(prev => Future(prev + i))
    }
  )

  @Benchmark
  def sumIntsR: Int = Await.result(
    numbers.foldLeft(Rerunnable(0)) {
      case (acc, i) => acc.flatMap(prev => Rerunnable(prev + i))
    }.run
  )

  @Benchmark
  def sumIntsT: Int = Await.result(
    numbers.foldLeft(Task(0)) {
      case (acc, i) => acc.flatMap(prev => Task(prev + i))
    }.run
  )

  @Benchmark
  def sumIntsPF: Int = Await.result(
    numbers.foldLeft(pool(0)) {
      case (acc, i) => acc.flatMap(prev => pool(prev + i))
    }
  )

  @Benchmark
  def sumIntsPR: Int = Await.result(
    numbers.foldLeft(Rerunnable.withFuturePool(pool)(0)) {
      case (acc, i) => acc.flatMap(prev => Rerunnable.withFuturePool(pool)(prev + i))
    }.run
  )

  @Benchmark
  def sumIntsPT: Int = Await.result(
    numbers.foldLeft(Task.fork(pool)(Task(0))) {
      case (acc, i) => acc.flatMap(prev => Task.fork(pool)(Task(prev + i)))
    }.run
  )
}
