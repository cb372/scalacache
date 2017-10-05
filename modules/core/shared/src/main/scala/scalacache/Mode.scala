package scalacache

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.Try

/**
  * Users must import a mode in order to specify the effect monad they want to use.
  *
  * The reason for having two effect monads, F and G, is to allow users to interact
  * with async cache implementations in a synchronous way, e.g. by awaiting a Future.
  *
  * @tparam F An effect that is compatible with the cache implementation being used.
  *           e.g. for an async cache this might be Future or IO.
  *           For a sync cache it is probably Id.
  *
  * @tparam G The effect monad that will wrap the return value of any cache operations.
  *
  * @tparam S Type class that describes the type of cache (sync or async).
  *           For async cache implementations, an async mode must be used.
  */
trait Mode[F[_], G[_], +S[X[_]] <: Sync[X]] {

  /**
    * The type class describing the inner effect (sync or async)
    */
  def S: S[F]

  def M: Monad[G]

  /**
    * Natural transformation from the internal effect monad to the user-facing one.
    */
  def transform[A](fa: F[A]): G[A]

}

/**
  * A mode in which the internal effect monad is the same as the user facing one.
  */
trait SimpleMode[F[_], +S[X[_]] <: Sync[X]] extends Mode[F, F, S] {

  def transform[A](fa: F[A]): F[A] = fa

}

object modes {

  object sync {

    type Id[X] = X

    /**
      * The simplest possible mode: just return the value as-is, without wrapping it in any effect monad.
      *
      * This mode is only compatible with synchronous cache implementations, e.g. caffeine, guava and ehcache.
      */
    implicit val id: Mode[Id, Id, Sync] = new SimpleMode[Id, Sync] {
      val M: Monad[Id] = MonadForId
      val S: Sync[Id] = SyncForId
    }

    /**
      * A mode for using async cache implementations (e.g. memcached) in a synchronous way.
      *
      * This mode is compatible with both sync and async cache implementations,
      * but if you are using a sync cache implementation then [[id]] mode is recommended
      * because it will be more efficient than this one.
      */
    implicit def await(implicit executionContext: ExecutionContext): Mode[Future, Id, Async] =
      new Mode[Future, Id, Async] {
        val M: Monad[Id] = MonadForId
        val S: Async[Future] = new AsyncForFuture
        def transform[A](fa: Future[A]): Id[A] = Await.result(fa, Duration.Inf)
      }
  }

  object try_ {

    /**
      * A mode for wrapping synchronous cache operations in [[scala.util.Try]].
      *
      * This mode is only compatible with synchronous cache implementations, e.g. caffeine, guava and ehcache.
      */
    implicit val mode: Mode[Try, Try, Sync] = new SimpleMode[Try, Sync] {
      val M: Monad[Try] = MonadForTry
      val S: Sync[Try] = SyncForTry
    }

  }

  object scalaFuture {

    /**
      * A mode for wrapping asynchronous cache operations in [[scala.concurrent.Future]].
      *
      * This mode is compatible with both sync and async cache implementations.
      */
    implicit def mode(implicit executionContext: ExecutionContext): Mode[Future, Future, Async] =
      new SimpleMode[Future, Async] {
        val M: Monad[Future] = new MonadForFuture
        val S: Async[Future] = new AsyncForFuture
      }
  }

}
