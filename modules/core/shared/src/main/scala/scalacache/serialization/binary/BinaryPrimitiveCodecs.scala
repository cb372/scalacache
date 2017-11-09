package scalacache.serialization.binary

import scalacache.serialization.Codec
import scalacache.serialization.Codec._

/**
  * Codecs for all the Java primitive types, plus String and Array[Byte]
  *
  * Credit: Shade @ https://github.com/alexandru/shade/blob/master/src/main/scala/shade/memcached/Codec.scala
  */
trait BinaryPrimitiveCodecs {

  implicit object IntBinaryCodec extends Codec[Int] {
    def encode(value: Int): Array[Byte] =
      Array(
        (value >>> 24).asInstanceOf[Byte],
        (value >>> 16).asInstanceOf[Byte],
        (value >>> 8).asInstanceOf[Byte],
        value.asInstanceOf[Byte]
      )

    def decode(data: Array[Byte]): DecodingResult[Int] = tryDecode(
      (data(0).asInstanceOf[Int] & 255) << 24 |
        (data(1).asInstanceOf[Int] & 255) << 16 |
        (data(2).asInstanceOf[Int] & 255) << 8 |
        data(3).asInstanceOf[Int] & 255
    )
  }

  implicit object DoubleBinaryCodec extends Codec[Double] {
    import java.lang.{Double => JvmDouble}
    def encode(value: Double): Array[Byte] = {
      val l = JvmDouble.doubleToLongBits(value)
      LongBinaryCodec.encode(l)
    }

    def decode(data: Array[Byte]): DecodingResult[Double] = {
      LongBinaryCodec
        .decode(data)
        .right
        .map(l => JvmDouble.longBitsToDouble(l))
    }
  }

  implicit object FloatBinaryCodec extends Codec[Float] {
    import java.lang.{Float => JvmFloat}
    def encode(value: Float): Array[Byte] = {
      val i = JvmFloat.floatToIntBits(value)
      IntBinaryCodec.encode(i)
    }

    def decode(data: Array[Byte]): DecodingResult[Float] = {
      IntBinaryCodec
        .decode(data)
        .right
        .map(i => JvmFloat.intBitsToFloat(i))
    }
  }

  implicit object LongBinaryCodec extends Codec[Long] {
    def encode(value: Long): Array[Byte] =
      Array(
        (value >>> 56).asInstanceOf[Byte],
        (value >>> 48).asInstanceOf[Byte],
        (value >>> 40).asInstanceOf[Byte],
        (value >>> 32).asInstanceOf[Byte],
        (value >>> 24).asInstanceOf[Byte],
        (value >>> 16).asInstanceOf[Byte],
        (value >>> 8).asInstanceOf[Byte],
        value.asInstanceOf[Byte]
      )

    def decode(data: Array[Byte]): DecodingResult[Long] = tryDecode(
      (data(0).asInstanceOf[Long] & 255) << 56 |
        (data(1).asInstanceOf[Long] & 255) << 48 |
        (data(2).asInstanceOf[Long] & 255) << 40 |
        (data(3).asInstanceOf[Long] & 255) << 32 |
        (data(4).asInstanceOf[Long] & 255) << 24 |
        (data(5).asInstanceOf[Long] & 255) << 16 |
        (data(6).asInstanceOf[Long] & 255) << 8 |
        data(7).asInstanceOf[Long] & 255
    )
  }

  implicit object BooleanBinaryCodec extends Codec[Boolean] {
    def encode(value: Boolean): Array[Byte] =
      Array((if (value) 1 else 0).asInstanceOf[Byte])

    def decode(data: Array[Byte]): DecodingResult[Boolean] =
      tryDecode(data.isDefinedAt(0) && data(0) == 1)
  }

  implicit object CharBinaryCodec extends Codec[Char] {
    def encode(value: Char): Array[Byte] = Array(
      (value >>> 8).asInstanceOf[Byte],
      value.asInstanceOf[Byte]
    )

    def decode(data: Array[Byte]): DecodingResult[Char] = tryDecode(
      ((data(0).asInstanceOf[Int] & 255) << 8 |
        data(1).asInstanceOf[Int] & 255)
        .asInstanceOf[Char]
    )
  }

  implicit object ShortBinaryCodec extends Codec[Short] {
    def encode(value: Short): Array[Byte] = Array(
      (value >>> 8).asInstanceOf[Byte],
      value.asInstanceOf[Byte]
    )

    def decode(data: Array[Byte]): DecodingResult[Short] = tryDecode(
      ((data(0).asInstanceOf[Short] & 255) << 8 |
        data(1).asInstanceOf[Short] & 255)
        .asInstanceOf[Short]
    )
  }

  implicit object StringBinaryCodec extends Codec[String] {
    def encode(value: String): Array[Byte] = value.getBytes("UTF-8")
    def decode(data: Array[Byte]): DecodingResult[String] = tryDecode(new String(data, "UTF-8"))
  }

  implicit object ArrayByteBinaryCodec extends Codec[Array[Byte]] {
    def encode(value: Array[Byte]): Array[Byte] = value
    def decode(data: Array[Byte]): DecodingResult[Array[Byte]] = Right(data)
  }
}
