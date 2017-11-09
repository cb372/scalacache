package scalacache

import scala.util.control.NonFatal
import scalaz.\/
import scalaz.concurrent.{Future, Task}

object Scalaz72 {

  object modes {

    implicit val task: Mode[Task] = new Mode[Task] {
      val M: Async[Task] = AsyncForScalazTask
    }

  }

  val AsyncForScalazTask: Async[Task] = new Async[Task] {

    def pure[A](a: A): Task[A] = Task.now(a)

    def map[A, B](fa: Task[A])(f: (A) => B): Task[B] = fa.map(f)

    def flatMap[A, B](fa: Task[A])(f: (A) => Task[B]): Task[B] = fa.flatMap(f)

    def raiseError[A](t: Throwable): Task[A] = Task.fail(t)

    def handleNonFatal[A](fa: => Task[A])(f: Throwable => A): Task[A] = fa.handle {
      case NonFatal(e) => f(e)
    }

    def delay[A](thunk: => A): Task[A] = Task.delay(thunk)

    def suspend[A](thunk: => Task[A]): Task[A] = Task.suspend(thunk)

    def async[A](register: (Either[Throwable, A] => Unit) => Unit): Task[A] =
      new Task(Future.async(register).map(\/.fromEither))

  }

}
