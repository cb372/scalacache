package scalacache

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}
import scala.util.Try

/**
  * When using ScalaCache you must import a mode in order to specify the effect monad
  * in which you want to wrap your computations.
  *
  * @tparam F The effect monad that will wrap the return value of any cache operations.
  *           e.g. [[scalacache.modes.sync.Id]], [[scala.concurrent.Future]], [[scala.util.Try]] or cats-effect IO.
  */
trait Mode[F[_]] {

  def M: Async[F]

}

object modes {

  object sync {

    /**
      * The simplest possible mode: just return the value as-is, without wrapping it in any effect monad.
      */
    implicit val mode: Mode[Id] = new Mode[Id] {
      val M: Async[Id] = AsyncForId
    }

  }

  object try_ {

    /**
      * A mode for wrapping synchronous cache operations in [[scala.util.Try]].
      */
    implicit val mode: Mode[Try] = new Mode[Try] {
      val M: Async[Try] = AsyncForTry
    }

  }

  object scalaFuture {

    /**
      * A mode for wrapping asynchronous cache operations in [[scala.concurrent.Future]].
      */
    implicit def mode(implicit executionContext: ExecutionContext): Mode[Future] =
      new Mode[Future] {
        val M: Async[Future] = new AsyncForFuture
      }
  }

}
