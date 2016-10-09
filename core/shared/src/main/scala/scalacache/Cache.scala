package scalacache

import scala.concurrent.duration.Duration
import scalacache.serialization.Codec

trait Cache[Repr] {

  /**
   * Get the value corresponding to the given key from the cache
   *
   * @param key cache key
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  def get[V](key: String)(implicit codec: Codec[V, Repr]): Option[V]

  /**
   * Insert the given key-value pair into the cache, with an optional Time To Live.
   *
   * @param key cache key
   * @param value corresponding value
   * @param ttl Time To Live
   * @tparam V the type of the corresponding value
   */
  def put[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V, Repr]): Unit

  /**
   * Remove the given key and its associated value from the cache, if it exists.
   * If the key is not in the cache, do nothing.
   *
   * @param key cache key
   */
  def remove(key: String): Unit

  /**
   * Delete the entire contents of the cache. Use wisely!
   */
  def removeAll(): Unit

  /**
   * You should call this when you have finished using this Cache.
   * (e.g. when your application shuts down)
   *
   * It will take care of gracefully shutting down the underlying cache client.
   *
   * Note that you should not try to use this Cache instance after you have called this method.
   */
  def close(): Unit

}

