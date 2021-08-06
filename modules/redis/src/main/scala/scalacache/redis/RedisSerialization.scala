package scalacache.redis

import scalacache.serialization.Codec
import scalacache.serialization.binary.BinaryCodec

/**
  * Custom serialization for caching arbitrary objects in Redis.
  * Ints, Longs, Doubles, Strings and byte arrays are treated specially.
  * Everything else is serialized using standard Java serialization.
  */
trait RedisSerialization {

  def serialize[A](value: A)(implicit codec: BinaryCodec[A]): Array[Byte] =
    codec.encode(value)

  def deserialize[A](bytes: Array[Byte])(implicit codec: BinaryCodec[A]): Codec.DecodingResult[A] =
    codec.decode(bytes)

}
