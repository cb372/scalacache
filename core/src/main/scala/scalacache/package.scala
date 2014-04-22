import scala.concurrent.duration.Duration

/**
 * Author: chris
 * Created: 4/21/14
 */
package object scalacache {

  /**
   * Get the value corresponding to the given key from the cache
   * @param key cache key
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  def get[V](key: String)(implicit scalaCache: ScalaCache): Option[V] =
    scalaCache.cache.get(key)

  /**
   * Insert the given key-value pair into the cache, with an optional Time To Live.
   * @param key cache key
   * @param value corresponding value
   * @param ttl Time To Live (optional, if not specified then the entry will last until it is naturally evicted)
   * @tparam V the type of the corresponding value
   */
  def put[V](key: String, value: V, ttl: Option[Duration] = None)(implicit scalaCache: ScalaCache): Unit =
    scalaCache.cache.put(key, value, ttl)

  /**
   * Remove the given key and its associated value from the cache, if it exists.
   * If the key is not in the cache, do nothing.
   * @param key cache key
   */
  def remove(key: String)(implicit scalaCache: ScalaCache): Unit =
    scalaCache.cache.remove(key)

  /**
   * Wrap the given block with a caching decorator.
   * First look in the cache. If the value is found, then return it immediately.
   * Otherwise run the block and save the result in the cache before returning it.
   * @param key cache key
   * @param ttl Time To Live (optional, if not specified then the entry will last until it is naturally evicted)
   * @param f the block to run
   * @tparam V the type of the block's result
   * @return the result, either retrived from the cache or returned by the block
   */
  def withCaching[V](key: String, ttl: Option[Duration] = None)(f: => V)(implicit scalaCache: ScalaCache): V = {
    scalaCache.cache.get(key) getOrElse {
      val result = f
      scalaCache.cache.put(key, result, ttl)
      result
    }
  }

}
