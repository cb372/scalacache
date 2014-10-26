package scalacache

import java.security.MessageDigest

/**
 * Sealed [[HashingAlgorithm]] trait to prevent users from shooting themselves in the foot at
 * runtime by specifying a crappy/unsupported algorithm name
 *
 * The name should be a valid MessageDigest algorithm name.Implementing child classes/objects should
 * refer to this list for proper names:
 *
 * http://docs.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html#MessageDigest
 */
sealed trait HashingAlgorithm {

  /**
   * Name of the algorithm
   */
  def name: String

  private final val tLocalMessageDigest: ThreadLocal[MessageDigest] = new ThreadLocal[MessageDigest] {
    override protected def initialValue(): MessageDigest = java.security.MessageDigest.getInstance(name)
  }

  /**
   * Returns a [[java.lang.ThreadLocal]] instance of [[MessageDigest]] that implements the hashing
   * algorithm specified by the "name" string.
   *
   * Since it is an unshared [[java.lang.ThreadLocal]] instance, calling various methods on the
   * [[java.security.MessageDigest]] returned by this method is "thread-safe".
   */
  final def messageDigest: MessageDigest = tLocalMessageDigest.get()
}

/**
 * MD5 returns 32 character long hexadecimal hash strings
 */
case object MD5 extends HashingAlgorithm {
  val name = "MD5"
}

/**
 * SHA1 returns 40 character long hexadecimal hash strings
 */
case object SHA1 extends HashingAlgorithm {
  val name = "SHA-1"
}

/**
 * SHA256 returns 64 character long hexadecimal hash strings
 */
case object SHA256 extends HashingAlgorithm {
  val name = "SHA-256"
}

/**
 * SHA512 returns 128 character long hexadecimal hash strings
 */
case object SHA512 extends HashingAlgorithm {
  val name = "SHA-512"
}
