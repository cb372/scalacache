package scalacache.logging
import scala.collection.mutable
import scala.scalajs.js.Dynamic.global

object Logger {

  private val loggers: mutable.Map[String, Logger] = mutable.Map.empty

  def getLogger(name: String): Logger = loggers.getOrElseUpdate(name, new Logger(name))

}

final class Logger(name: String) {

  def isDebugEnabled: Boolean = true

  def isWarnEnabled: Boolean = true

  def debug(message: String): Unit =
    global.console.debug(s"$name: $message")

  def warn(message: String, e: Throwable): Unit =
    global.console.warn(s"$name: $message. Exception: $e")

}
