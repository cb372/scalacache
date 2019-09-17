package scalacache

import _root_.zio.Task

import scala.util.control.NonFatal

object zio {

  object modes {

    implicit val task: Mode[Task] = new Mode[Task] {
      val M: Async[Task] = AsyncForZIOTask
    }

  }

  val AsyncForZIOTask: Async[Task] = new Async[Task] {

    def pure[A](a: A): Task[A] = Task.succeed(a)

    def map[A, B](fa: Task[A])(f: (A) => B): Task[B] = fa.map(f)

    def flatMap[A, B](fa: Task[A])(f: (A) => Task[B]): Task[B] = fa.flatMap(f)

    def raiseError[A](t: Throwable): Task[A] = Task.fail(t)

    def handleNonFatal[A](fa: => Task[A])(f: Throwable => A): Task[A] = fa.catchSome {
      case NonFatal(e) => Task.succeed(f(e))
    }

    def delay[A](thunk: => A): Task[A] = Task.effect(thunk)

    def suspend[A](thunk: => Task[A]): Task[A] = Task.flatten(Task.effect(thunk))

    def async[A](register: (Either[Throwable, A] => Unit) => Unit): Task[A] =
      Task.effectAsync { (kk: Task[A] => Unit) =>
        register { e =>
          kk {
            e match {
              case Left(t)  => Task.fail(t)
              case Right(r) => Task.succeed(r)
            }
          }
        }
      }

  }
}
