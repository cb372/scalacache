package scalacache.redis

import scalacache.serialization.Codec
import scodec.bits.ByteVector

/**
  * Custom serialization for caching arbitrary objects in Redis.
  * Ints, Longs, Doubles, Strings and byte arrays are treated specially.
  * Everything else is serialized using standard Java serialization.
  */
trait RedisSerialization {

  def serialize[A](value: A)(implicit codec: Codec[A]): ByteVector =
    codec.encode(value)

  def deserialize[A](bytes: ByteVector)(implicit codec: Codec[A]): Codec.DecodingResult[A] =
    codec.decode(bytes)

  def deserialize[A](bytes: Array[Byte])(implicit codec: Codec[A]): Codec.DecodingResult[A] =
    codec.decode(bytes)

}
