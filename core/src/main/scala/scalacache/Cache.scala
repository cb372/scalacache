package scalacache

import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }

trait Cache {

  /**
   * Get the value corresponding to the given key from the cache
   * @param key cache key
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  def get[V](key: String)(implicit execContext: ExecutionContext): Future[Option[V]]

  /**
   * Insert the given key-value pair into the cache, with an optional Time To Live.
   * @param key cache key
   * @param value corresponding value
   * @param ttl Time To Live
   * @tparam V the type of the corresponding value
   */
  def put[V](key: String, value: V, ttl: Option[Duration])(implicit execContext: ExecutionContext): Future[Unit]

  /**
   * Remove the given key and its associated value from the cache, if it exists.
   * If the key is not in the cache, do nothing.
   * @param key cache key
   */
  def remove(key: String)(implicit execContext: ExecutionContext): Future[Unit]

}

