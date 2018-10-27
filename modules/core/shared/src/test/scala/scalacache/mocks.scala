package scalacache

import scalacache.logging.Logger
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration
import scala.language.higherKinds

class EmptyCache[V](implicit val config: CacheConfig) extends AbstractCache[V] {

  override protected def logger = Logger.getLogger("EmptyCache")

  override protected def doGet[F[_]](key: String)(implicit mode: Mode[F]) =
    mode.M.pure(None)

  override protected def doPut[F[_]](key: String, value: V, ttl: Option[Duration])(implicit mode: Mode[F]) =
    mode.M.pure(())

  override protected def doRemove[F[_]](key: String)(implicit mode: Mode[F]) =
    mode.M.pure(())

  override protected def doRemoveAll[F[_]]()(implicit mode: Mode[F]) =
    mode.M.pure(())

  override def close[F[_]]()(implicit mode: Mode[F]) = mode.M.pure(())

}

class FullCache[V](value: V)(implicit val config: CacheConfig) extends AbstractCache[V] {

  override protected def logger = Logger.getLogger("FullCache")

  override protected def doGet[F[_]](key: String)(implicit mode: Mode[F]) =
    mode.M.pure(Some(value))

  override protected def doPut[F[_]](key: String, value: V, ttl: Option[Duration])(implicit mode: Mode[F]) =
    mode.M.pure(())

  override protected def doRemove[F[_]](key: String)(implicit mode: Mode[F]) =
    mode.M.pure(())

  override protected def doRemoveAll[F[_]]()(implicit mode: Mode[F]) =
    mode.M.pure(())

  override def close[F[_]]()(implicit mode: Mode[F]) = mode.M.pure(())

}

class ErrorRaisingCache[V](implicit val config: CacheConfig) extends AbstractCache[V] {

  override protected def logger = Logger.getLogger("FullCache")

  override protected def doGet[F[_]](key: String)(implicit mode: Mode[F]) =
    mode.M.raiseError(new RuntimeException("failed to read"))

  override protected def doPut[F[_]](key: String, value: V, ttl: Option[Duration])(implicit mode: Mode[F]) =
    mode.M.raiseError(new RuntimeException("failed to write"))

  override protected def doRemove[F[_]](key: String)(implicit mode: Mode[F]) =
    mode.M.pure(())

  override protected def doRemoveAll[F[_]]()(implicit mode: Mode[F]) =
    mode.M.pure(())

  override def close[F[_]]()(implicit mode: Mode[F]) = mode.M.pure(())

}

/**
  * A mock cache for use in tests and samples.
  * Does not support TTL.
  */
class MockCache[V](implicit val config: CacheConfig) extends AbstractCache[V] {

  override protected def logger = Logger.getLogger("MockCache")

  val mmap = collection.mutable.Map[String, V]()

  override protected def doGet[F[_]](key: String)(implicit mode: Mode[F]) =
    mode.M.delay(mmap.get(key))

  override protected def doPut[F[_]](key: String, value: V, ttl: Option[Duration])(implicit mode: Mode[F]) =
    mode.M.delay(mmap.put(key, value))

  override protected def doRemove[F[_]](key: String)(implicit mode: Mode[F]) =
    mode.M.delay(mmap.remove(key))

  override protected def doRemoveAll[F[_]]()(implicit mode: Mode[F]) =
    mode.M.delay(mmap.clear())

  override def close[F[_]]()(implicit mode: Mode[F]) = mode.M.pure(())

}

/**
  * A cache that keeps track of the arguments it was called with. Useful for tests.
  * Designed to be mixed in as a stackable trait.
  */
trait LoggingCache[V] extends AbstractCache[V] {
  var (getCalledWithArgs, putCalledWithArgs, removeCalledWithArgs) =
    (ArrayBuffer.empty[String], ArrayBuffer.empty[(String, Any, Option[Duration])], ArrayBuffer.empty[String])

  protected abstract override def doGet[F[_]](key: String)(implicit mode: Mode[F]): F[Option[V]] = {
    getCalledWithArgs.append(key)
    super.doGet(key)
  }

  protected abstract override def doPut[F[_]](key: String, value: V, ttl: Option[Duration])(
      implicit mode: Mode[F]): F[Any] = {
    putCalledWithArgs.append((key, value, ttl))
    super.doPut(key, value, ttl)
  }

  protected abstract override def doRemove[F[_]](key: String)(implicit mode: Mode[F]): F[Any] = {
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
class LoggingMockCache[V] extends MockCache[V] with LoggingCache[V]
