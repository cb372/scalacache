package scalacache.ehcache

import net.sf.ehcache.{Element, Cache => Ehcache}
import org.slf4j.LoggerFactory
import scalacache.serialization.Codec
import scalacache.{AbstractCache, Async, CacheConfig}

import scala.concurrent.duration.Duration
import scala.language.higherKinds

/**
  * Thin wrapper around Ehcache.
  */
class EhcacheCache[F[_]](override val underlying: Ehcache)(implicit val config: CacheConfig, F: Async[F])
    extends AbstractCache[F] {

  override type Underlying = Ehcache

  override protected final val logger =
    LoggerFactory.getLogger(getClass.getName)

  override protected def doGet[V: Codec](key: String): F[Option[V]] = {
    F.delay {
      val result = {
        val elem = underlying.get(key)
        if (elem == null) None
        else Option(elem.getObjectValue.asInstanceOf[V])
      }
      logCacheHitOrMiss(key, result)
      result
    }
  }

  override protected def doPut[V: Codec](key: String, value: V, ttl: Option[Duration]): F[Unit] = {
    F.delay {
      val element = new Element(key, value)
      ttl.foreach(t => element.setTimeToLive(t.toSeconds.toInt))
      underlying.put(element)
      logCachePut(key, ttl)
    }
  }

  override protected def doRemove(key: String): F[Unit] = F.delay(underlying.remove(key))
  override protected def doRemoveAll(): F[Unit] = F.delay(underlying.removeAll())
  override def close(): F[Unit] = F.pure(()) // Nothing to do

}

object EhcacheCache {

  /**
    * Create a new cache utilizing the given underlying Ehcache cache.
    *
    * @param underlying an Ehcache cache
    */
  def apply[F[_]: Async](underlying: Ehcache)(implicit config: CacheConfig): EhcacheCache[F] =
    new EhcacheCache[F](underlying)

}
