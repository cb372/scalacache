package scalacache

import java.nio.charset.StandardCharsets
import java.time.{Clock, Instant}

/**
  * A cache entry with an optional expiry time
  */
final case class Entry(value: Array[Byte], expiresAt: Option[Instant]) {

  /**
    * Has the entry expired yet?
    */
  def isExpired(implicit clock: Clock): Boolean = expiresAt.exists(_.isBefore(Instant.now(clock)))

}

object Entry {
  final def apply(value: String, expiresAt: Option[Instant]): Entry =
    new Entry(value.getBytes(StandardCharsets.UTF_8), expiresAt)
}
