package scalacache

import cats.{ Id, Monad }

import scala.annotation.implicitNotFound
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.higherKinds
import scala.util.Try

//@implicitNotFound(
//  msg = """Could not find ScalaCache's mode in implicit scope. Please add one of the following imports:
//  import scalacache.modes.throwExceptions
//  import scalacache.modes.wrapWithTry
//  import scalacache.modes.future
//"""
//)
sealed abstract class Mode[F[_]](implicit val fm: Monad[F]) {
  def wrap[T](x: => T): F[T]
}

object modes {

  implicit val throwExceptions = IdMode
  implicit val wrapWithTry = TryMode
  implicit def runAsFuture(implicit ec: ExecutionContext) = FutureMode()(ec)

  case object IdMode extends Mode[Id] {
    def wrap[T](x: => T): Id[T] = x
  }

  case object TryMode extends Mode[Try]()(cats.instances.try_.catsStdInstancesForTry) {
    def wrap[T](x: => T): Try[T] = Try(x)
  }

  case class FutureMode(implicit val ec: ExecutionContext) extends Mode[Future]()(cats.instances.future.catsStdInstancesForFuture(ec)) {
    def wrap[T](x: => T): Future[T] = Future(x)
  }

}
