/*
 * Copyright 2021 scalacache
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package scalacache.memcached

import scalacache._

/** Trait that you can use to define your own Memcached key sanitiser
  */
trait MemcachedKeySanitizer {

  /** Converts a string to a valid Memcached key
    */
  def toValidMemcachedKey(key: String): String
}

/** Sanitizer that replaces characters invalid for Memcached and truncates keys if they are over a certain limit.
  *
  * Convenient because it creates human-readable keys, but only safe for ASCII chars.
  *
  * @param replacementChar
  *   optional, defaults to an underscore
  * @param maxKeyLength
  *   optional, defaults to 250, which is the max length of a Memcached key
  */
case class ReplaceAndTruncateSanitizer(replacementChar: String = "_", maxKeyLength: Int = 250)
    extends MemcachedKeySanitizer {

  val invalidCharsRegex = "[^\u0021-\u007e]".r

  /** Convert the given string to a valid Memcached key by:
    *   - replacing all invalid characters with underscores
    *   - truncating the string to 250 characters
    *
    * From the Memcached protocol spec:
    *
    * Data stored by memcached is identified with the help of a key. A key is a text string which should uniquely
    * identify the data for clients that are interested in storing and retrieving it. Currently the length limit of a
    * key is set at 250 characters (of course, normally clients wouldn't need to use such long keys); the key must not
    * include control characters or whitespace.
    *
    * Because of the structure of cache keys, the most useful information is likely to be at the right hand end, so
    * truncation is performed from the left.
    */
  def toValidMemcachedKey(key: String): String = {
    val replacedKey = invalidCharsRegex.replaceAllIn(key, replacementChar)
    if (replacedKey.size <= maxKeyLength) replacedKey
    else replacedKey.substring(replacedKey.size - maxKeyLength)
  }

}

/** [[HashingMemcachedKeySanitizer]] uses the provided [[scalacache.HashingAlgorithm]] to create a valid Memcached key
  * using characters in hexadecimal. You may want to use this [[MemcachedKeySanitizer]] if there is a possibility that
  * your keys will contain non-ASCII characters.
  *
  * Make sure that the [[HashingAlgorithm]] you provide does not produce strings that are beyond 250 characters when
  * combined with any additional namespacing that your MemcachedClient or proxy automatically inserts for you.
  */
case class HashingMemcachedKeySanitizer(algorithm: HashingAlgorithm = MD5) extends MemcachedKeySanitizer {

  /** Uses the specified hashing algorithm to digest a key and spit out a hexidecimal representation of the hashed key
    */
  def toValidMemcachedKey(key: String): String =
    algorithm.messageDigest
      .digest(key.getBytes("UTF-8"))
      .map("%02x".format(_))
      .mkString
}
