package scalacache.redis

import redis.clients.jedis._

import scala.collection.JavaConverters._
import scala.language.higherKinds
import scalacache.{CacheConfig, Flags, Mode}
import scalacache.serialization.Codec

/**
  * Thin wrapper around Jedis
  */
class RedisCache[V](val jedisPool: JedisPool)(implicit val config: CacheConfig, val codec: Codec[V])
    extends RedisCacheBase[V] {

  type JClient = Jedis

  import StringEnrichment.StringWithUtf8Bytes

  protected def doRemoveAll[F[_]]()(implicit mode: Mode[F]): F[Any] = mode.M.delay {
    val jedis = jedisPool.getResource()
    try {
      jedis.flushDB()
    } finally {
      jedis.close()
    }
  }

  protected def toKey(keyParts: Seq[Any]): String = config.cacheKeyBuilder.toCacheKey(keyParts)

  def getMultiple[F[_]](keysParts: Seq[Seq[Any]])(implicit mode: Mode[F], flags: Flags): F[Seq[Option[V]]] =
    if (keysParts.isEmpty) mode.M.pure(Seq.empty)
    else {
      val keys = keysParts.map(toKey)
      if (flags.readsEnabled) {
        mode.M.suspend {
          withJedis { jedis =>
            val keysAsBytes  = keys.map(_.utf8bytes)
            val bytesForKeys = jedis.mget(keysAsBytes: _*)
            val results: Seq[Codec.DecodingResult[Option[V]]] = bytesForKeys.asScala.map(bytes => {
              if (bytes != null)
                codec.decode(bytes).right.map(Some(_))
              else
                Right(None)
            })
            val errors = results.collect { case Left(e) => e }
            if (errors.nonEmpty) {
              mode.M.raiseError(
                new RuntimeException(
                  s"Errors when decoding cached values: " + errors.mkString(System.lineSeparator),
                  errors.head
                )
              )
            } else {
              mode.M.pure(results.zipWithIndex.collect {
                case (Right(maybeValue), i) =>
                  if (logger.isDebugEnabled) logCacheHitOrMiss(keys(i), maybeValue)
                  maybeValue
              })
            }
          }
        }
      } else {
        if (logger.isDebugEnabled) {
          logger.debug(s"Skipping cache GET because cache reads are disabled. Keys: ${keys.mkString(", ")}")
        }
        mode.M.pure(keys.map(_ => None))
      }
    }

  def putMultiple[F[_]](keysPartsAndValues: Seq[(Seq[Any], V)])(implicit mode: Mode[F], flags: Flags): F[Any] =
    if (keysPartsAndValues.isEmpty) mode.M.pure(Unit)
    else {
      if (flags.writesEnabled) {
        mode.M.delay {
          withJedis { jedis =>
            jedis.mset(keysPartsAndValues.flatMap { case (k, v) => Seq(toKey(k).utf8bytes, codec.encode(v)) }: _*)
          }
          if (logger.isDebugEnabled) keysPartsAndValues.foreach { case (k, _) => logCachePut(toKey(k), None) }
        }
      } else {
        if (logger.isDebugEnabled) {
          logger.debug(s"Skipping cache PUT because cache writes are disabled. Keys: ${keysPartsAndValues
            .map { case (key, _) => toKey(key) }
            .mkString(", ")}")
        }
        mode.M.pure(())
      }
    }

  def removeMultiple[F[_]](keysParts: Seq[Seq[Any]])(implicit mode: Mode[F]): F[Any] =
    if (keysParts.isEmpty) mode.M.pure(Unit)
    else
      mode.M.delay {
        withJedis { jedis =>
          jedis.del(keysParts.map(toKey): _*)
        }
      }

}

object RedisCache {

  /**
    * Create a Redis client connecting to the given host and use it for caching
    */
  def apply[V](host: String, port: Int)(implicit config: CacheConfig, codec: Codec[V]): RedisCache[V] =
    apply(new JedisPool(host, port))

  /**
    * Create a cache that uses the given Jedis client pool
    * @param jedisPool a Jedis pool
    */
  def apply[V](jedisPool: JedisPool)(implicit config: CacheConfig, codec: Codec[V]): RedisCache[V] =
    new RedisCache[V](jedisPool)

}
