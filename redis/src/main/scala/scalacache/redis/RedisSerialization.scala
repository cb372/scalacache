package scalacache.redis

import scalacache.serdes.Codec

/**
 * Custom serialization for caching arbitrary objects in Redis.
 * Ints, Longs, Doubles, Strings and byte arrays are treated specially.
 * Everything else is serialized using standard Java serialization.
 */
trait RedisSerialization {

  protected def customClassloader: Option[ClassLoader] = None

  def serialize[A: Codec](value: A): Array[Byte] = {
    implicitly[Codec[A]].serialize(value)
  }

  def deserialize[A: Codec](bytes: Array[Byte]): A = {
    implicitly[Codec[A]].deserialize(bytes)
  }

}
