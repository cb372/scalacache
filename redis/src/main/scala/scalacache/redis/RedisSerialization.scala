package scalacache.redis

import scalacache.serdes.Codec

/**
 * Custom serialization for caching arbitrary objects in Redis.
 * Ints, Longs, Doubles, Strings and byte arrays are treated specially.
 * Everything else is serialized using standard Java serialization.
 */
trait RedisSerialization {

  protected def customClassloader: Option[ClassLoader] = None

  def serialize[A](value: A)(implicit codec: Codec[A]): Array[Byte] = {
    codec.serialize(value)
  }

  def deserialize[A](bytes: Array[Byte])(implicit codec: Codec[A]): A = {
    codec.deserialize(bytes)
  }

}
