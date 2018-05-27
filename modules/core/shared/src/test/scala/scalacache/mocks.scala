package scalacache

import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration
import scala.language.higherKinds
import scalacache.serialization.Codec

class EmptyCache[F[_]](implicit val config: CacheConfig, mode: Mode[F]) extends AbstractCache[F] {

  override protected val logger = LoggerFactory.getLogger("EmptyCache")

  override protected def doGet[V: Codec](key: String) =
    mode.M.pure(None)

  override protected def doPut[V: Codec](key: String, value: V, ttl: Option[Duration]) =
    mode.M.pure(())

  override protected def doRemove(key: String) =
    mode.M.pure(())

  override protected def doRemoveAll() =
    mode.M.pure(())

  override def close() = mode.M.pure(())

}

class FullCache[F[_]](value: Any)(implicit val config: CacheConfig, mode: Mode[F]) extends AbstractCache[F] {

  override protected val logger = LoggerFactory.getLogger("FullCache")

  override protected def doGet[V: Codec](key: String) =
    mode.M.pure(Some(value).asInstanceOf[Option[V]])

  override protected def doPut[V: Codec](key: String, value: V, ttl: Option[Duration]) =
    mode.M.pure(())

  override protected def doRemove(key: String) =
    mode.M.pure(())

  override protected def doRemoveAll() =
    mode.M.pure(())

  override def close() = mode.M.pure(())

}

class ErrorRaisingCache[F[_]](implicit val config: CacheConfig, mode: Mode[F]) extends AbstractCache[F] {

  override protected val logger = LoggerFactory.getLogger("FullCache")

  override protected def doGet[V: Codec](key: String) =
    mode.M.raiseError(new RuntimeException("failed to read"))

  override protected def doPut[V: Codec](key: String, value: V, ttl: Option[Duration]) =
    mode.M.raiseError(new RuntimeException("failed to write"))

  override protected def doRemove(key: String) =
    mode.M.pure(())

  override protected def doRemoveAll() =
    mode.M.pure(())

  override def close() = mode.M.pure(())

}

/**
  * A mock cache for use in tests and samples.
  * Does not support TTL.
  */
class MockCache[F[_]]()(implicit val config: CacheConfig, mode: Mode[F]) extends AbstractCache[F] {

  override protected val logger = LoggerFactory.getLogger("MockCache")

  val mmap = collection.mutable.Map[String, Any]()

  override protected def doGet[V: Codec](key: String) =
    mode.M.delay(mmap.get(key).asInstanceOf[Option[V]])

  override protected def doPut[V: Codec](key: String, value: V, ttl: Option[Duration]) =
    mode.M.delay(mmap.put(key, value))

  override protected def doRemove(key: String) =
    mode.M.delay(mmap.remove(key))

  override protected def doRemoveAll() =
    mode.M.delay(mmap.clear())

  override def close() = mode.M.pure(())

}

/**
  * A cache that keeps track of the arguments it was called with. Useful for tests.
  * Designed to be mixed in as a stackable trait.
  */
trait LoggingCache[F[_]] extends AbstractCache[F] {
  var (getCalledWithArgs, putCalledWithArgs, removeCalledWithArgs) =
    (ArrayBuffer.empty[String], ArrayBuffer.empty[(String, Any, Option[Duration])], ArrayBuffer.empty[String])

  protected abstract override def doGet[V: Codec](key: String): F[Option[V]] = {
    getCalledWithArgs.append(key)
    super.doGet(key)
  }

  protected abstract override def doPut[V: Codec](key: String, value: V, ttl: Option[Duration]): F[Unit] = {
    putCalledWithArgs.append((key, value, ttl))
    super.doPut(key, value, ttl)
  }

  protected abstract override def doRemove(key: String): F[Any] = {
    removeCalledWithArgs.append(key)
    super.doRemove(key)
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
class LoggingMockCache[F[_]: Mode] extends MockCache[F] with LoggingCache[F]
