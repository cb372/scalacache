package scalacache.memcached

/**
 *
 * Author: c-birchall
 * Date:   13/11/07
 */
class MemcachedKeySanitizer(replacementChar: String = "_", maxKeyLength: Int = 250) {

  val invalidCharsRegex = """[^a-zA-Z0-9~`!@#$%^&*()_+=\[\]{}\\|:;"',.<>?/-]""".r

  /**
   * Convert the given string to a valid Memcached key by:
   *   - replacing all invalid characters with underscores
   *   - truncating the string to 250 characters
   *
   * From the Memcached protocol spec:
   *
   * Data stored by memcached is identified with the help of a key. A key
   * is a text string which should uniquely identify the data for clients
   * that are interested in storing and retrieving it.  Currently the
   * length limit of a key is set at 250 characters (of course, normally
   * clients wouldn't need to use such long keys); the key must not include
   * control characters or whitespace.
   *
   * Because of the structure of cache keys, the most useful information
   * is likely to be at the right hand end, so truncation is performed from the left.
   */
  def toValidMemcachedKey(key: String): String = {
    val replacedKey = invalidCharsRegex.replaceAllIn(key, replacementChar)
    if (replacedKey.size <= maxKeyLength) replacedKey
    else replacedKey.substring(replacedKey.size - maxKeyLength)
  }

}
