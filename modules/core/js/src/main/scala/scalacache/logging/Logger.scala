package scalacache.logging
import scala.collection.mutable
import scala.scalajs.js.Dynamic.global
import cats.effect.Sync
import cats.effect.SyncIO
import cats.~>
import cats.implicits._
import cats.effect.SyncEffect

object Logger {
  def getLogger[F[_]: Sync](name: String): Logger[F] = new Logger[F](name)
}

final class Logger[F[_]: Sync](name: String) {
  def ifDebugEnabled[A](fa: => F[A]): F[Option[A]] = fa.map(_.some)

  def ifWarnEnabled[A](fa: => F[A]): F[Option[A]] = fa.map(_.some)

  def debug(message: String): F[Unit] =
    Sync[F].delay(global.console.debug(s"$name: $message"))

  def warn(message: String, e: Throwable): F[Unit] =
    Sync[F].delay(global.console.warn(s"$name: $message. Exception: $e"))
}
