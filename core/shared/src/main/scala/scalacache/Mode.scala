package scalacache

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}
import scala.util.{Success, Try}

//@implicitNotFound(
//  msg = """Could not find ScalaCache's mode in implicit scope. Please add one of the following imports:
//  import scalacache.modes.throwExceptions
//  import scalacache.modes.wrapWithTry
//  import scalacache.modes.runAsFuture
//"""
//)
abstract class Mode {
  type F[Res]
  def pure[T](x: T): F[T]
  def wrap[T](x: => T): F[T]
  def map[A, B](fa: F[A])(f: A => B): F[B]
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
}

object Mode {
  type Aux[M[_]] = Mode { type F[Res] = M[Res] }
}

object modes {

  implicit val throwExceptions = IdMode
  implicit val wrapWithTry = TryMode
  implicit def runAsFuture(implicit ec: ExecutionContext) = FutureMode()(ec)

  case object IdMode extends Mode {
    type F[Res] = Res
    def pure[T](x: T): T = x
    def wrap[T](x: => T): T = x
    def map[A, B](x: A)(f: A => B): B = f(x)
    def flatMap[A, B](x: A)(f: A => B): B = f(x)
  }

  case object TryMode extends Mode {
    type F[Res] = Try[Res]
    def pure[T](x: T): Try[T] = Success(x)
    def wrap[T](x: => T): Try[T] = Try(x)
    def map[A, B](fa: Try[A])(f: A => B): Try[B] = fa.map(f)
    def flatMap[A, B](fa: Try[A])(f: A => Try[B]): Try[B] = fa.flatMap(f)
  }

  case class FutureMode(implicit ec: ExecutionContext) extends Mode {
    type F[Res] = Future[Res]
    def pure[T](x: T): Future[T] = Future.successful(x)
    def wrap[T](x: => T): Future[T] = Future(x)
    def map[A, B](fa: Future[A])(f: A => B): Future[B] = fa.map(f)
    def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = fa.flatMap(f)
  }

}
