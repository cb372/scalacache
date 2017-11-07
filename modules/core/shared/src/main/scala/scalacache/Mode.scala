package scalacache

import scala.annotation.implicitNotFound
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}
import scala.util.Try

/**
  * When using ScalaCache you must import a mode in order to specify the effect monad
  * in which you want to wrap your computations.
  *
  * @tparam F The effect monad that will wrap the return value of any cache operations.
  *           e.g. [[scalacache.Id]], [[scala.concurrent.Future]], [[scala.util.Try]] or cats-effect IO.
  */
@implicitNotFound(msg = """Could not find a Mode for type ${F}.

If you want synchronous execution, try importing the sync mode:

import scalacache.modes.sync._

If you are working with Scala Futures, import the scalaFuture mode
and don't forget you will also need an ExecutionContext:

import scalacache.modes.scalaFuture._
import scala.concurrent.ExecutionContext.Implicits.global
 """)
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
