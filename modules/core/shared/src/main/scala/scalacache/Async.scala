package scalacache

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.language.higherKinds
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

trait MonadError[F[_]] {

  def pure[A](a: A): F[A]

  def map[A, B](fa: F[A])(f: A => B): F[B]

  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  def raiseError[A](t: Throwable): F[A]

  // Note: the argument is by-name only for the sake of the Id instance.
  // A bit hacky but it works, at the expense of some overhead for allocating a thunk
  // even when other implementations don't need it.
  def handleNonFatal[A](fa: => F[A])(f: Throwable => A): F[A]

}

trait Sync[F[_]] extends MonadError[F] {

  def delay[A](thunk: => A): F[A]

  def suspend[A](thunk: => F[A]): F[A]

}

trait Async[F[_]] extends Sync[F] {

  def async[A](register: (Either[Throwable, A] => Unit) => Unit): F[A]

}

object AsyncForId extends Async[Id] {

  def pure[A](a: A): Id[A] = a

  def map[A, B](fa: Id[A])(f: A => B): Id[B] = f(fa)

  def flatMap[A, B](fa: Id[A])(f: A => Id[B]): Id[B] = f(fa)

  def delay[A](thunk: => A): Id[A] = thunk

  def suspend[A](thunk: => Id[A]): Id[A] = thunk

  def raiseError[A](t: Throwable): Id[A] = throw t

  def handleNonFatal[A](fa: => Id[A])(f: Throwable => A): Id[A] = {
    try {
      fa
    } catch {
      case NonFatal(e) => f(e)
    }
  }

  def async[A](register: (Either[Throwable, A] => Unit) => Unit): Id[A] = {
    val promise = Promise[A]()
    register {
      case Left(e)  => promise.failure(e)
      case Right(x) => promise.success(x)
    }
    Await.result(promise.future, Duration.Inf)
  }

}

object AsyncForTry extends Async[Try] {

  def pure[A](a: A): Try[A] = Success(a)

  def map[A, B](fa: Try[A])(f: A => B): Try[B] = fa.map(f)

  def flatMap[A, B](fa: Try[A])(f: A => Try[B]): Try[B] = fa.flatMap(f)

  def delay[A](thunk: => A): Try[A] = Try(thunk)

  def suspend[A](thunk: => Try[A]): Try[A] = thunk

  def raiseError[A](t: Throwable): Try[A] = Failure(t)

  def async[A](register: (Either[Throwable, A] => Unit) => Unit): Try[A] = {
    val promise = Promise[A]()
    register {
      case Left(e)  => promise.failure(e)
      case Right(x) => promise.success(x)
    }
    Try(Await.result(promise.future, Duration.Inf))
  }

  def handleNonFatal[A](fa: => Try[A])(f: Throwable => A): Try[A] = {
    fa.recover {
      case NonFatal(e) => f(e)
    }
  }

}

class AsyncForFuture(implicit ec: ExecutionContext) extends Async[Future] {

  def pure[A](a: A): Future[A] = Future.successful(a)

  def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)

  def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)

  def delay[A](thunk: => A): Future[A] = Future(thunk)

  def suspend[A](thunk: => Future[A]): Future[A] = thunk

  def raiseError[A](t: Throwable): Future[A] = Future.failed(t)

  def async[A](register: (Either[Throwable, A] => Unit) => Unit): Future[A] = {
    val promise = Promise[A]()
    register {
      case Left(e)  => promise.failure(e)
      case Right(x) => promise.success(x)
    }
    promise.future
  }

  def handleNonFatal[A](fa: => Future[A])(f: Throwable => A): Future[A] = {
    fa.recover {
      case NonFatal(e) => f(e)
    }
  }

}
