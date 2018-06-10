package scalacache

import cats.effect.{Async => CatsAsync}

import scala.language.higherKinds
import scala.util.control.NonFatal

object CatsEffect {

  object implicits {

    /**
      * A mode that wraps computations in F[_],
      * where there is an instance of cats-effect Async available for F.
      */
    implicit final def async[F[_]: CatsAsync]: Async[F] = asyncForCatsEffectAsync[F]

  }

  private[scalacache] final def asyncForCatsEffectAsync[F[_]](implicit F: CatsAsync[F]): Async[F] = new Async[F] {

    def pure[A](a: A): F[A] = F.pure(a)

    def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = F.flatMap(fa)(f)

    def map[A, B](fa: F[A])(f: A => B): F[B] = F.map(fa)(f)

    def raiseError[A](t: Throwable): F[A] = F.raiseError(t)

    def handleNonFatal[A](fa: F[A])(f: Throwable => A): F[A] = F.recover(fa) { case NonFatal(e) => f(e) }

    def delay[A](thunk: => A): F[A] = F.delay(thunk)

    def suspend[A](thunk: => F[A]): F[A] = F.suspend(thunk)

    def async[A](register: (Either[Throwable, A] => Unit) => Unit): F[A] = F.async(register)

  }

}
