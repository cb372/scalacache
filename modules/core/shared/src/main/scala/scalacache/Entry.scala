package scalacache

import java.time.{Clock, Instant}

import scodec.bits.ByteVector

/**
  * A cache entry with an optional expiry time
  */
final case class Entry(value: ByteVector, expiresAt: Option[Instant]) {

  /**
    * Has the entry expired yet?
    */
  def isExpired(implicit clock: Clock): Boolean = expiresAt.exists(_.isBefore(Instant.now(clock)))

}

object Entry {
  def apply(value: String, expiresAt: Option[Instant]): Entry =
    new Entry(ByteVector.encodeUtf8(value).right.get, expiresAt)
}
