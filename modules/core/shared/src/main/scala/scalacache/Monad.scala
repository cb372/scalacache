package scalacache

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.{Success, Try}
import scalacache.modes.sync.Id

trait Monad[F[_]] {

  def pure[A](a: A): F[A]

  def map[A, B](fa: F[A])(f: A => B): F[B]

  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

}

object MonadForId extends Monad[Id] {

  def pure[A](a: A): Id[A] = a

  def map[A, B](fa: Id[A])(f: A => B): Id[B] = f(fa)

  def flatMap[A, B](fa: Id[A])(f: A => Id[B]): Id[B] = f(fa)

}

object MonadForTry extends Monad[Try] {

  def pure[A](a: A): Try[A] = Success(a)

  def map[A, B](fa: Try[A])(f: A => B): Try[B] = fa.map(f)

  def flatMap[A, B](fa: Try[A])(f: A => Try[B]): Try[B] = fa.flatMap(f)

}

class MonadForFuture(implicit ec: ExecutionContext) extends Monad[Future] {

  def pure[A](a: A): Future[A] = Future.successful(a)

  def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)

  def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa.flatMap(f)

}
