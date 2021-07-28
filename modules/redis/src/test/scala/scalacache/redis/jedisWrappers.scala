package scalacache.redis

import redis.clients.jedis._

/**
  * Scala wrapper for Jedis implementations. This allows an implementation of [[RedisCacheSpecBase]] to choose
  * the specific client required for running the tests.
  */
trait BaseJedisClient {

  def set(key: Array[Byte], value: Array[Byte]): Unit

  def get(key: Array[Byte]): Array[Byte]

  def get(key: String): String

  def ttl(key: Array[Byte]): Long

  def pttl(key: String): Long

}

class JedisClusterClient(val underlying: JedisCluster) extends BaseJedisClient {

  override def set(key: Array[Byte], value: Array[Byte]): Unit =
    underlying.set(key, value)

  override def get(key: Array[Byte]): Array[Byte] =
    underlying.get(key)

  override def get(key: String): String =
    underlying.get(key)

  override def ttl(key: Array[Byte]): Long =
    underlying.ttl(key)

  override def pttl(key: String): Long =
    underlying.pttl(key)
}

class JedisClient(val underlying: Jedis) extends BaseJedisClient {

  override def set(key: Array[Byte], value: Array[Byte]): Unit =
    underlying.set(key, value)

  override def get(key: Array[Byte]): Array[Byte] =
    underlying.get(key)

  override def get(key: String): String =
    underlying.get(key)

  override def ttl(key: Array[Byte]): Long =
    underlying.ttl(key)

  override def pttl(key: String): Long =
    underlying.pttl(key)
}

class ShardedJedisClient(val underlying: ShardedJedis) extends BaseJedisClient {

  override def set(key: Array[Byte], value: Array[Byte]): Unit =
    underlying.set(key, value)

  override def get(key: Array[Byte]): Array[Byte] =
    underlying.get(key)

  override def get(key: String): String =
    underlying.get(key)

  override def ttl(key: Array[Byte]): Long =
    underlying.ttl(key)

  override def pttl(key: String): Long =
    underlying.pttl(key)
}
