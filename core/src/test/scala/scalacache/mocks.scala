package scalacache

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scalacache.serialization.Codec

class EmptyCache extends Cache {
  override def get[V](key: String)(implicit codec: Codec[V]): Future[Option[V]] = Future.successful(None)
  override def put[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V]) = Future.successful((): Unit)
  override def remove(key: String) = Future.successful((): Unit)
  override def removeAll() = Future.successful((): Unit)
  override def close(): Unit = {}
}

class FullCache(value: Any) extends Cache {
  override def get[V](key: String)(implicit codec: Codec[V]): Future[Option[V]] = Future.successful(Some(value).asInstanceOf[Option[V]])
  override def put[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V]) = Future.successful((): Unit)
  override def remove(key: String) = Future.successful((): Unit)
  override def removeAll() = Future.successful((): Unit)
  override def close(): Unit = {}
}

class FailedFutureReturningCache extends Cache {
  override def get[V](key: String)(implicit codec: Codec[V]): Future[Option[V]] = Future.failed(new RuntimeException("failed to read"))
  override def put[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V]): Future[Unit] = Future.failed(new RuntimeException("failed to write"))
  override def remove(key: String) = Future.successful((): Unit)
  override def removeAll() = Future.successful((): Unit)
  override def close(): Unit = {}
}

/**
 * A mock cache for use in tests and samples.
 * Does not support TTL.
 */
class MockCache extends Cache {

  val mmap = collection.mutable.Map[String, Any]()

  def get[V](key: String)(implicit codec: Codec[V]) = {
    val value = mmap.get(key)
    Future.successful(value.asInstanceOf[Option[V]])
  }

  def put[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V]) =
    Future.successful(mmap.put(key, value))

  def remove(key: String) =
    Future.successful(mmap.remove(key))

  def removeAll() =
    Future.successful(mmap.clear())

  def close(): Unit = {}

}

/**
 * A cache that keeps track of the arguments it was called with. Useful for tests.
 * Designed to be mixed in as a stackable trait.
 */
trait LoggingCache extends Cache {
  var (getCalledWithArgs, putCalledWithArgs, removeCalledWithArgs) = (
    ArrayBuffer.empty[String],
    ArrayBuffer.empty[(String, Any, Option[Duration])],
    ArrayBuffer.empty[String])

  abstract override def get[V](key: String)(implicit codec: Codec[V]): Future[Option[V]] = {
    getCalledWithArgs.append(key)
    super.get(key)
  }

  abstract override def put[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V]) = {
    putCalledWithArgs.append((key, value, ttl))
    super.put(key, value, ttl)
  }

  abstract override def remove(key: String) = {
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
