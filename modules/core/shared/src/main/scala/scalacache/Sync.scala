package scalacache

import scala.language.higherKinds

// TODO delete this and just use Async?
trait Sync[F[_]] extends Monad[F] {

  def delay[A](thunk: => A): F[A]

  // TODO is this needed?
  def raiseError[A](t: Throwable): F[A]

}

