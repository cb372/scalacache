package scalacache

import cats.effect.Sync
import scalacache.logging.Logger
import scalacache.memoization.MemoizationConfig

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration
import scala.language.higherKinds

class EmptyCache[F[_], V](implicit val F: Sync[F], val config: MemoizationConfig) extends AbstractCache[F, String, V] {

  override protected def logger = Logger.getLogger("EmptyCache")

  override protected def doGet(key: String) =
    F.pure(None)

  override protected def doPut(key: String, value: V, ttl: Option[Duration]) =
    F.unit

  override protected def doRemove(key: String) =
    F.unit

  override protected val doRemoveAll =
    F.unit

  override val close = F.unit

}

class FullCache[F[_], V](value: V)(implicit val F: Sync[F], val config: MemoizationConfig)
    extends AbstractCache[F, String, V] {

  override protected def logger = Logger.getLogger("FullCache")

  override protected def doGet(key: String) =
    F.pure(Some(value))

  override protected def doPut(key: String, value: V, ttl: Option[Duration]) =
    F.unit

  override protected def doRemove(key: String) =
    F.unit

  override protected val doRemoveAll =
    F.unit

  override val close = F.unit

}

class ErrorRaisingCache[F[_], V](implicit val F: Sync[F], val config: MemoizationConfig)
    extends AbstractCache[F, String, V] {

  override protected val logger = Logger.getLogger("FullCache")

  override protected def doGet(key: String) =
    F.raiseError(new RuntimeException("failed to read"))

  override protected def doPut(key: String, value: V, ttl: Option[Duration]) =
    F.raiseError(new RuntimeException("failed to write"))

  override protected def doRemove(key: String) =
    F.unit

  override protected val doRemoveAll =
    F.unit

  override val close = F.unit

}

/** A mock cache for use in tests and samples. Does not support TTL.
  */
class MockCache[F[_], V](implicit val F: Sync[F], val config: MemoizationConfig) extends AbstractCache[F, String, V] {

  override protected def logger = Logger.getLogger("MockCache")

  val mmap = collection.mutable.Map[String, V]()

  override protected def doGet(key: String) =
    F.delay(mmap.get(key))

  override protected def doPut(key: String, value: V, ttl: Option[Duration]) =
    F.delay(mmap.put(key, value))

  override protected def doRemove(key: String) =
    F.delay(mmap.remove(key))

  override protected val doRemoveAll =
    F.delay(mmap.clear())

  override val close = F.unit

}

/** A cache that keeps track of the arguments it was called with. Useful for tests. Designed to be mixed in as a
  * stackable trait.
  */
trait LoggingCache[F[_], V] extends AbstractCache[F, String, V] {
  val F: Sync[F]

  var (getCalledWithArgs, putCalledWithArgs, removeCalledWithArgs) =
    (ArrayBuffer.empty[String], ArrayBuffer.empty[(String, Any, Option[Duration])], ArrayBuffer.empty[String])

  protected abstract override def doGet(key: String): F[Option[V]] = F.defer {
    getCalledWithArgs.append(key)
    super.doGet(key)
  }

  protected abstract override def doPut(key: String, value: V, ttl: Option[Duration]): F[Unit] = F.defer {
    putCalledWithArgs.append((key, value, ttl))
    super.doPut(key, value, ttl)
  }

  protected abstract override def doRemove(key: String): F[Unit] = F.defer {
    removeCalledWithArgs.append(key)
    super.doRemove(key)
  }

  val reset: F[Unit] = F.delay {
    getCalledWithArgs.clear()
    putCalledWithArgs.clear()
    removeCalledWithArgs.clear()
  }

}

/** A mock cache that keeps track of the arguments it was called with.
  */
class LoggingMockCache[F[_]: Sync, V] extends MockCache[F, V] with LoggingCache[F, V]
