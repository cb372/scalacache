package scalacache

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future }

/**
 * A mock cache for use in tests and samples.
 * Does not support TTL.
 *
 * Author: c-birchall
 * Date:   13/11/07
 */
class MockCache extends Cache {

  val mmap = collection.mutable.Map[String, Any]()

  def get[V](key: String)(implicit execContext: ExecutionContext) = {
    val value = mmap.get(key)
    Future.successful(value.asInstanceOf[Option[V]])
  }

  def put[V](key: String, value: V, ttl: Option[Duration])(implicit execContext: ExecutionContext) =
    Future.successful(mmap.put(key, value))

  def remove(key: String)(implicit execContext: ExecutionContext) =
    Future.successful(mmap.remove(key))
}

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

  abstract override def get[V](key: String)(implicit execContext: ExecutionContext): Future[Option[V]] = {
    getCalledWithArgs.append(key)
    super.get(key)
  }

  abstract override def put[V](key: String, value: V, ttl: Option[Duration])(implicit execContext: ExecutionContext) = {
    putCalledWithArgs.append((key, value, ttl))
    super.put(key, value, ttl)
  }

  abstract override def remove(key: String)(implicit execContext: ExecutionContext) = {
    removeCalledWithArgs.append(key)
    super.remove(key)
  }

  def reset(): Unit = {
    getCalledWithArgs.clear()
    putCalledWithArgs.clear()
    removeCalledWithArgs.clear()
  }

}

/**
 * A mock cache that keeps track of the arguments it was called with.
 */
class LoggingMockCache extends MockCache with LoggingCache
