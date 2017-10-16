package scalacache

import scala.language.higherKinds

trait Monad[F[_]] {

  def pure[A](a: A): F[A]

  def map[A, B](fa: F[A])(f: A => B): F[B]

  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

}
