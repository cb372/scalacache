package scalacache

import scala.concurrent.duration.Duration

/**
 * A mock cache for use in tests and samples.
 * Does not support TTL.
 *
 * Author: c-birchall
 * Date:   13/11/07
 */
class MockCache extends Cache {

  val mmap = collection.mutable.Map[String, Any]()

  def get[V](key: String): Option[V] = {
    val value = mmap.get(key)
    value.asInstanceOf[Option[V]]
  }

  def put[V](key: String, value: V, ttl: Option[Duration]): Unit = mmap.put(key, value)

  def remove(key: String): Unit = mmap.remove(key)
}
