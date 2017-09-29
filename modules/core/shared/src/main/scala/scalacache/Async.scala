package scalacache

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.higherKinds

trait Async[F[_]] extends Sync[F] {
  def async[A](register: (Either[Throwable, A] => Unit) => Unit): F[A]
}

class AsyncForFuture(implicit ec: ExecutionContext) extends Async[Future] {

  def pure[A](a: A): Future[A] = Future.successful(a)

  def delay[A](thunk: => A): Future[A] = Future(thunk)

  def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)

  def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] =
    fa.flatMap(f)

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
