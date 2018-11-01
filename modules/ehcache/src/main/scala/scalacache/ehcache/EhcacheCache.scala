package scalacache.ehcache

import scalacache.{AbstractCache, CacheConfig, Mode}
import scalacache.logging.Logger

import scala.concurrent.duration.Duration
import net.sf.ehcache.{Element, Cache => Ehcache}

import scala.language.higherKinds

/**
  * Thin wrapper around Ehcache.
  */
class EhcacheCache[V](val underlying: Ehcache)(implicit val config: CacheConfig) extends AbstractCache[V] {

  override protected final val logger = Logger.getLogger(getClass.getName)

  override protected def doGet[F[_]](key: String)(implicit mode: Mode[F]): F[Option[V]] = {
    mode.M.delay {
      val result = {
        val elem = underlying.get(key)
        if (elem == null) None
        else Option(elem.getObjectValue.asInstanceOf[V])
      }
      logCacheHitOrMiss(key, result)
      result
    }
  }

  override protected def doPut[F[_]](key: String, value: V, ttl: Option[Duration])(implicit mode: Mode[F]): F[Any] = {
    mode.M.delay {
      val element = new Element(key, value)
      ttl.foreach(t => element.setTimeToLive(t.toSeconds.toInt))
      underlying.put(element)
      logCachePut(key, ttl)
    }
  }

  override protected def doRemove[F[_]](key: String)(implicit mode: Mode[F]): F[Any] =
    mode.M.delay(underlying.remove(key))

  override protected def doRemoveAll[F[_]]()(implicit mode: Mode[F]): F[Any] =
    mode.M.delay(underlying.removeAll())

  override def close[F[_]]()(implicit mode: Mode[F]): F[Any] = {
    // Nothing to do
    mode.M.pure(())
  }

}

object EhcacheCache {

  /**
    * Create a new cache utilizing the given underlying Ehcache cache.
    *
    * @param underlying an Ehcache cache
    */
  def apply[V](underlying: Ehcache)(implicit config: CacheConfig): EhcacheCache[V] =
    new EhcacheCache[V](underlying)

}
