package scalacache

import scalacache.modes.sync.Id
import scala.language.higherKinds
import scala.util.{Failure, Success, Try}

trait Sync[F[_]] {

  def delay[A](thunk: => A): F[A]

  // TODO is this needed?
  def raiseError[A](t: Throwable): F[A]

}

object SyncForId extends Sync[Id] {

  def delay[A](thunk: => A): Id[A] = thunk

  def raiseError[A](t: Throwable): Id[A] = throw t

}

object SyncForTry extends Sync[Try] {

  def delay[A](thunk: => A): Try[A] = Try(thunk)

  def raiseError[A](t: Throwable): Try[A] = Failure(t)

}
