package scalacache.caffeine

import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.effect.{Clock, Sync}
import cats.implicits._
import com.github.benmanes.caffeine.cache.{Caffeine, Cache => CCache}
import scalacache.logging.Logger
import scalacache.{AbstractCache, CacheConfig, Entry}

import java.time.Instant
import scala.concurrent.duration.Duration
import scala.language.higherKinds

/*
 * Thin wrapper around Caffeine.
 */
class CaffeineCache[F[_]: Async, V](val underlying: CCache[String, Entry[V]])(
    implicit val config: CacheConfig,
    clock: Clock[F]
) extends AbstractCache[F, V] {
  protected val F: Sync[F] = Sync[F]

  override protected final val logger = Logger.getLogger(getClass.getName)

  def doGet(key: String): F[Option[V]] = {
    F.delay {
        Option(underlying.getIfPresent(key))
      }
      .flatMap(_.filterA(Entry.isExpired[F, V]))
      .map(_.map(_.value))
      .flatTap { result =>
        logCacheHitOrMiss(key, result)
      }
  }

  def doPut(key: String, value: V, ttl: Option[Duration]): F[Unit] =
    ttl.traverse(toExpiryTime).flatMap { expiry =>
      F.delay {
        val entry = Entry(value, expiry)
        underlying.put(key, entry)
      } *> logCachePut(key, ttl)
    }

  override def doRemove(key: String): F[Unit] =
    F.delay(underlying.invalidate(key))

  override def doRemoveAll(): F[Unit] =
    F.delay(underlying.invalidateAll())

  override def doCachingF(key: String)(f: F[V])(ttl: Option[Duration]): F[V] = {
    val doCachingFAtomically: F[Entry[V]] =
      Dispatcher[F]
        .use(disp =>
          F.blocking(
            underlying
              .asMap()
              // guarantees atomicity, see Caffeine javadoc and implementation
              .compute(
                key,
                (_, entryNullable) => {
                  val newEntry = (ttl.traverse(toExpiryTime), f).mapN((e, v) => Entry(v, e))
                  val fe = Option(entryNullable) match {
                    case Some(entry @ Entry(_, Some(_))) =>
                      Entry.isExpired(entry).ifM(newEntry, F.pure(entry))
                    case Some(Entry(v, None)) => F.pure(Entry(v, None))
                    case None                 => newEntry
                  }
                  // only JVM
                  // consider basing the implementation on caffeine's AsyncCache instead
                  disp.unsafeRunSync(fe)
                }
              )
          )
        )

    for {
      entryOpt <- F.delay(Option(underlying.getIfPresent(key)))
      entry <- entryOpt match {
        case Some(entry @ Entry(_, Some(_))) => Entry.isExpired(entry).ifM(doCachingFAtomically, F.pure(entry))
        case Some(e @ Entry(_, None))        => F.pure(e)
        case None                            => doCachingFAtomically
      }
    } yield entry.value
  }

  override def close: F[Unit] = {
    // Nothing to do
    F.unit
  }

  private def toExpiryTime(ttl: Duration): F[Instant] =
    clock.monotonic.map(m => Instant.ofEpochMilli(m.toMillis).plusMillis(ttl.toMillis))
}

object CaffeineCache {

  /**
    * Create a new Caffeine cache.
    */
  def apply[F[_]: Async: Clock, V](implicit config: CacheConfig): F[CaffeineCache[F, V]] =
    Sync[F].delay(Caffeine.newBuilder().build[String, Entry[V]]()).map(apply(_))

  /**
    * Create a new cache utilizing the given underlying Caffeine cache.
    *
    * @param underlying a Caffeine cache
    */
  def apply[F[_]: Async: Clock, V](
      underlying: CCache[String, Entry[V]]
  )(implicit config: CacheConfig): CaffeineCache[F, V] =
    new CaffeineCache(underlying)
}
