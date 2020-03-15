package io.catbird.util.effect

import java.lang.{ Runnable, Thread }
import java.util.concurrent.{ Executors, ThreadFactory }

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutorService }

trait ThreadPoolNamingSupport {

  def newNamedThreadPool(name: String): ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(
      Executors.newSingleThreadExecutor(new ThreadFactory {
        override def newThread(r: Runnable): Thread = {
          val thread = Executors.defaultThreadFactory().newThread(r)
          thread.setName(name)
          thread.setDaemon(true) // Don't block shutdown of JVM
          thread
        }
      })
    )

  def currentThreadName(): String = Thread.currentThread().getName
}
