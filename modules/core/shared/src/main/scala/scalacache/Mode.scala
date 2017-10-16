package scalacache

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.Try

/**
  * When using ScalaCache you must import a mode in order to specify the effect monad
  * in which you want to wrap your computations.
  *
  * @tparam F The effect monad that will wrap the return value of any cache operations.
  *           This effect monad must be compatible with the cache implementation being used.
  *           e.g. for an async cache this might be Future or IO.
  *           For a sync cache it is probably Id.
  *
  * @tparam M Type class that describes the type of cache (sync or async).
  *           For async cache implementations, an async mode must be used.
  */
trait Mode[F[_], +M[X[_]] <: Sync[X]] {

  // TODO get rid of the Sync/Async abstraction and just use Async?

  /**
    * The type class describing the effect monad (sync or async)
    */
  def M: M[F]

}

object modes {

  object sync {

    type Id[X] = X

    /**
      * The simplest possible mode: just return the value as-is, without wrapping it in any effect monad.
      */
    implicit val mode: Mode[Id, Async] = new Mode[Id, Async] {
      val M: Async[Id] = AsyncForId
    }

  }

  object try_ {

    /**
      * A mode for wrapping synchronous cache operations in [[scala.util.Try]].
      *
      * This mode is only compatible with synchronous cache implementations, e.g. caffeine, guava and ehcache.
      */
    implicit val mode: Mode[Try, Async] = new Mode[Try, Async] {
      val M: Async[Try] = AsyncForTry
    }

  }

  object scalaFuture {

    /**
      * A mode for wrapping asynchronous cache operations in [[scala.concurrent.Future]].
      *
      * This mode is compatible with both sync and async cache implementations.
      */
    implicit def mode(implicit executionContext: ExecutionContext): Mode[Future, Async] =
      new Mode[Future, Async] {
        val M: Async[Future] = new AsyncForFuture
      }
  }

}
