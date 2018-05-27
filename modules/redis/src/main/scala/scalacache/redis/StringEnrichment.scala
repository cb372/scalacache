package scalacache.redis

import java.nio.charset.StandardCharsets

object StringEnrichment {

  /**
    * Enrichment class to convert String to UTF-8 byte array
    */
  implicit final class StringWithUtf8Bytes(val string: String) extends AnyVal {
    def utf8bytes: Array[Byte] = string.getBytes(StandardCharsets.UTF_8)
  }

}
