package scalacache.redis

import java.nio.charset.Charset

object StringEnrichment {

  private val utf8 = Charset.forName("UTF-8")

  /**
    * Enrichment class to convert String to UTF-8 byte array
    */
  implicit class StringWithUtf8Bytes(val string: String) extends AnyVal {
    def utf8bytes = string.getBytes(utf8)
  }

}
