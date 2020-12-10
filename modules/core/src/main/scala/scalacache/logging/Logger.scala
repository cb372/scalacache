package scalacache.logging

import org.slf4j.{Logger => Slf4jLogger, LoggerFactory}
import cats.effect.Sync
import cats.Applicative
import cats.implicits._

object Logger {
  def getLogger[F[_]: Sync](name: String): Logger[F] = new Logger[F](LoggerFactory.getLogger(name))
}

final class Logger[F[_]: Sync](private val logger: Slf4jLogger) {
  private def whenM[A](fb: F[Boolean])(fa: => F[A]): F[Option[A]] = fb.ifM(fa.map(_.some), Applicative[F].pure(None))

  def ifDebugEnabled[A](fa: => F[A]): F[Option[A]] =
    whenM(Sync[F].delay(logger.isDebugEnabled))(fa)

  def ifWarnEnabled[A](fa: => F[A]): F[Option[A]] = whenM(Sync[F].delay(logger.isWarnEnabled))(fa)

  def debug(message: String): F[Unit] = Sync[F].delay(logger.debug(message))

  def warn(message: String): F[Unit] = Sync[F].delay(logger.warn(message))

  def warn(message: String, e: Throwable): F[Unit] = Sync[F].delay(logger.warn(message, e))

}
