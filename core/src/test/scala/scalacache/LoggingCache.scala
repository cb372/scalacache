package scalacache

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration

/**
 * A cache that keeps track of the arguments it was called with. Useful for tests.
 * Designed to be mixed in as a stackable trait.
 *
 * Author: c-birchall
 * Date:   2014/04/22
 */
trait LoggingCache extends Cache {
  var (getCalledWithArgs, putCalledWithArgs, removeCalledWithArgs) = (
    ArrayBuffer.empty[String],
    ArrayBuffer.empty[(String, Any, Option[Duration])],
    ArrayBuffer.empty[String])

  abstract override def get[V](key: String): Option[V] = {
    getCalledWithArgs.append(key)
    super.get(key)
  }

  abstract override def put[V](key: String, value: V, ttl: Option[Duration]): Unit = {
    putCalledWithArgs.append((key, value, ttl))
    super.put(key, value, ttl)
  }

  abstract override def remove(key: String): Unit = {
    removeCalledWithArgs.append(key)
    super.remove(key)
  }

  def reset(): Unit = {
    getCalledWithArgs.clear()
    putCalledWithArgs.clear()
    removeCalledWithArgs.clear()
  }

}

class LoggingMockCache extends MockCache with LoggingCache
