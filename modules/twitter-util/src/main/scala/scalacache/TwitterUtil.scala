package scalacache
import com.twitter.util.{Future, Promise}

import scala.util.control.NonFatal

object TwitterUtil {
  object modes {

    implicit val future: Mode[Future] = new Mode[Future] {
      val M: Async[Future] = AsyncForTwitterFuture
    }

  }

  object AsyncForTwitterFuture extends Async[Future] {

    def pure[A](a: A): Future[A] = Future.value(a)

    def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)

    def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)

    def delay[A](thunk: => A): Future[A] = Future(thunk)

    def suspend[A](thunk: => Future[A]): Future[A] = thunk

    def raiseError[A](t: Throwable): Future[A] = Future.exception(t)

    def async[A](register: (Either[Throwable, A] => Unit) => Unit): Future[A] = {
      val promise = Promise[A]()
      register {
        case Left(e)  => promise.raise(e)
        case Right(x) => promise.setValue(x)
      }
      promise
    }

    def handleNonFatal[A](fa: => Future[A])(f: Throwable => A): Future[A] = {
      fa.handle {
        case NonFatal(e) => f(e)
      }
    }

  }

}
