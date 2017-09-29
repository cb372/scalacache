package scalacache

import scalacache.modes.sync.Id
import scala.language.higherKinds
import scala.util.{Failure, Try}

trait Sync[F[_]] {

  def delay[A](thunk: => A): F[A]

  def map[A, B](fa: F[A])(f: A => B): F[B]

  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

  // TODO is this needed?
  def raiseError[A](t: Throwable): F[A]

}

object SyncForId extends Sync[Id] {

  def delay[A](thunk: => A): Id[A] = thunk

  def map[A, B](fa: Id[A])(f: A => B): Id[B] = f(fa)

  def flatMap[A, B](fa: Id[A])(f: A => Id[B]): Id[B] = f(fa)

  def raiseError[A](t: Throwable): Id[A] = throw t

}

object SyncForTry extends Sync[Try] {

  def delay[A](thunk: => A): Try[A] = Try(thunk)

  def map[A, B](fa: Try[A])(f: A => B): Try[B] = fa.map(f)

  def flatMap[A, B](fa: Try[A])(f: A => Try[B]): Try[B] = fa.flatMap(f)

  def raiseError[A](t: Throwable): Try[A] = Failure(t)

}

