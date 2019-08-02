package scalacache.mapdb

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant}

import org.mapdb._
import scalacache.logging.Logger
import scalacache.{AbstractCache, CacheConfig, Entry, Mode}

import scala.concurrent.duration.Duration

class MapDBCache[V](val underlying: HTreeMap[String, Entry[V]])(
    implicit val config: CacheConfig,
    clock: Clock = Clock.systemUTC()
) extends AbstractCache[V] {

  override protected final val logger = Logger.getLogger(getClass.getName)

  override protected def doGet[F[_]](key: String)(implicit mode: Mode[F]): F[Option[V]] = {
    mode.M.delay {
      val entry = underlying.get(key)
      val result = {
        if (entry == null || entry.isExpired)
          None
        else
          Some(entry.value)
      }
      logCacheHitOrMiss(key, result)
      result
    }
  }

  override protected def doPut[F[_]](key: String, value: V, ttl: Option[Duration])(implicit mode: Mode[F]): F[Any] = {
    mode.M.delay {
      val entry = Entry(value, ttl.map(toExpiryTime))
      underlying.put(key, entry: Entry[V])
      logCachePut(key, ttl)
    }
  }

  override protected def doRemove[F[_]](key: String)(implicit mode: Mode[F]): F[Any] =
    mode.M.delay(underlying.remove(key))

  override protected def doRemoveAll[F[_]]()(implicit mode: Mode[F]): F[Any] =
    mode.M.delay(underlying.clearWithoutNotification())

  override def close[F[_]]()(implicit mode: Mode[F]): F[Any] = mode.M.pure(())

  private def toExpiryTime(ttl: Duration): Instant =
    Instant.now(clock).plus(ttl.toMillis, ChronoUnit.MILLIS)
}

object MapDBCache {
  val db: DB = DBMaker.memoryDB.make

  Runtime
    .getRuntime()
    .addShutdownHook(new Thread(() => db.close()))

  /**
    * Create a new MapDB cache
    */
  def apply[V](implicit config: CacheConfig): MapDBCache[V] =
    apply(db.hashMap("map").createOrOpen.asInstanceOf[HTreeMap[String, Entry[V]]])

  /**
    * Create a new cache utilizing the given underlying MapDB cache.
    *
    * @param underlying a MapDB cache
    */
  def apply[V](underlying: HTreeMap[String, Entry[V]])(implicit config: CacheConfig): MapDBCache[V] =
    new MapDBCache(underlying)

}
