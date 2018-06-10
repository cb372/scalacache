package scalacache

import scala.language.higherKinds

trait MonadError[F[_]] {

  def pure[A](a: A): F[A]

  def map[A, B](fa: F[A])(f: A => B): F[B]

  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  def raiseError[A](t: Throwable): F[A]

  def handleNonFatal[A](fa: F[A])(f: Throwable => A): F[A]

}

trait Sync[F[_]] extends MonadError[F] {

  def delay[A](thunk: => A): F[A]

  def suspend[A](thunk: => F[A]): F[A]

}

trait Async[F[_]] extends Sync[F] {

  def async[A](register: (Either[Throwable, A] => Unit) => Unit): F[A]

}
