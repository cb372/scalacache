package scalacache

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.Try

trait Mode[F[_], +M[G[_]] <: Sync[G]] {

  def M: M[F]

}

object modes {

  object sync {

    type Id[X] = X

    implicit val mode: Mode[Id, Sync] = new Mode[Id, Sync] {
      val M: Sync[Id] = SyncForId
    }

  }

  object try_ {

    implicit val mode: Mode[Try, Sync] = new Mode[Try, Sync] {
      val M: Sync[Try] = SyncForTry
    }

  }

  object scalaFuture {
    implicit def mode(implicit executionContext: ExecutionContext): Mode[Future, Async] =
      new Mode[Future, Async] {
        val M: Async[Future] = new AsyncForFuture
      }
  }

}

