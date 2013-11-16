package cacheable

/**
 *
 * Author: c-birchall
 * Date:   13/11/07
 */
package object pkg {
  implicit var cacheConfig: CacheConfig = null

  def insidePackageObject(a: Int): Int = cacheable {
    123
  }

}
