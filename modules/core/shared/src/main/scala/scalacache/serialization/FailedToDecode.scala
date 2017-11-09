package scalacache.serialization

final case class FailedToDecode(cause: Throwable) extends Exception(cause)
