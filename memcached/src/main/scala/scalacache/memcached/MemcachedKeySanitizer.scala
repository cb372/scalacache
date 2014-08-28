package scalacache.memcached

import scalacache._

/**
 *
 * Author: c-birchall
 * Date:   13/11/07
 */

/**
 * Trait that you can use to define your own Memcached key serialiser
 */
trait MemcachedKeySanitizer {
  /**
   * Converts a string to a valid Memcached key
   */
  def toValidMemcachedKey(key: String): String
}

case class ReplaceAndTruncateSanitizer(replacementChar: String = "_",
                                       maxKeyLength: Int = 250)
    extends MemcachedKeySanitizer {

  val invalidCharsRegex = "[^\u0021-\u007e]".r

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

/**
 * [[HashingMemcachedKeySanitizer]] uses the provided [[HashingAlgorithm]] to create a valid Memcached key
 * using characters in hexadecimal.
 *
 * Make sure that the [[HashingAlgorithm]] you provide does not produce strings that are beyond 250 characters
 * when combined with any additional namespacing that your MemcachedClient or proxy automatically inserts for
 * you.
 */
case class HashingMemcachedKeySanitizer(algorithm: HashingAlgorithm = MD5) extends MemcachedKeySanitizer {
  private val messageDigest = java.security.MessageDigest.getInstance(algorithm.name)

  /**
   * Uses the specified hashing algorithm to digest a key and spit out a hexidecimal representation
   * of the hashed key
   */
  def toValidMemcachedKey(key: String): String = {
    messageDigest.digest(key.getBytes).map("%02x".format(_)).mkString
  }
}