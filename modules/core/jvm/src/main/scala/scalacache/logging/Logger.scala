package scalacache.logging

import org.slf4j.{Logger => Slf4jLogger, LoggerFactory}

object Logger {

  def getLogger(name: String): Logger = new Logger(LoggerFactory.getLogger(name))

}

final class Logger(logger: Slf4jLogger) {

  def isDebugEnabled: Boolean = logger.isDebugEnabled

  def isWarnEnabled: Boolean = logger.isWarnEnabled

  def debug(message: String): Unit = logger.debug(message)

  def warn(message: String): Unit = logger.warn(message)

  def warn(message: String, e: Throwable): Unit = logger.warn(message, e)

}
