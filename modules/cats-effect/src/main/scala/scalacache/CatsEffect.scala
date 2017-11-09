package scalacache

import cats.effect.{Async => CatsAsync, IO}

import scala.language.higherKinds
import scala.util.control.NonFatal

object CatsEffect {

  object modes {

    /**
      * A mode that wraps computations in cats-effect IO.
      */
    implicit val io: Mode[IO] = new Mode[IO] {
      val M: Async[IO] = asyncForCatsEffectAsync[IO]
    }

  }

  def asyncForCatsEffectAsync[F[_]](implicit af: CatsAsync[F]): Async[F] = new Async[F] {

    def pure[A](a: A): F[A] = af.pure(a)

    def flatMap[A, B](fa: F[A])(f: (A) => F[B]): F[B] = af.flatMap(fa)(f)

    def map[A, B](fa: F[A])(f: (A) => B): F[B] = af.map(fa)(f)

    def raiseError[A](t: Throwable): F[A] = af.raiseError(t)

    def handleNonFatal[A](fa: => F[A])(f: Throwable => A): F[A] = af.recover(fa) {
      case NonFatal(e) => f(e)
    }

    def delay[A](thunk: => A): F[A] = af.delay(thunk)

    def suspend[A](thunk: => F[A]): F[A] = af.suspend(thunk)

    def async[A](register: (Either[Throwable, A] => Unit) => Unit): F[A] = af.async(register)

  }

}
