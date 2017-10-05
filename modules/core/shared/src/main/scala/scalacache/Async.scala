package scalacache

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.higherKinds

trait Async[F[_]] extends Sync[F] {
  def async[A](register: (Either[Throwable, A] => Unit) => Unit): F[A]
}

class AsyncForFuture(implicit ec: ExecutionContext) extends Async[Future] {

  def delay[A](thunk: => A): Future[A] = Future(thunk)

  def raiseError[A](t: Throwable): Future[A] = Future.failed(t)

  def async[A](register: (Either[Throwable, A] => Unit) => Unit): Future[A] = {
    val promise = Promise[A]()
    register {
      case Left(t) => promise.failure(t)
      case Right(t) => promise.success(t)
    }
    promise.future
  }

}
