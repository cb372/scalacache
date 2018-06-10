package scalacache.serialization.binary

import java.nio.charset.StandardCharsets

import scalacache.serialization.Codec
import scalacache.serialization.Codec._

import scala.reflect.ClassTag

/**
  * Codecs for all the Java primitive types, plus String and Array[Byte]
  *
  * Credit: Shade @ https://github.com/alexandru/shade/blob/master/src/main/scala/shade/memcached/Codec.scala
  */
trait BinaryPrimitiveCodecs extends LowPriorityBinaryPrimitiveCodecs {

  implicit final object IntBinaryCodec extends Codec[Int] {
    override final def encode(value: Int): Array[Byte] =
      Array(
        (value >>> 24).asInstanceOf[Byte],
        (value >>> 16).asInstanceOf[Byte],
        (value >>> 8).asInstanceOf[Byte],
        value.asInstanceOf[Byte]
      )

    override final def decode(data: Array[Byte]): DecodingResult[Int] = tryDecode(
      (data(0).asInstanceOf[Int] & 255) << 24 |
        (data(1).asInstanceOf[Int] & 255) << 16 |
        (data(2).asInstanceOf[Int] & 255) << 8 |
        data(3).asInstanceOf[Int] & 255
    )
  }

}

trait LowPriorityBinaryPrimitiveCodecs extends LowerPriorityBinaryAnyRefCodecs {

  implicit final object DoubleBinaryCodec extends Codec[Double] {
    import java.lang.{Double => JvmDouble}
    override final def encode(value: Double): Array[Byte] = {
      val l = JvmDouble.doubleToLongBits(value)
      LongBinaryCodec.encode(l)
    }

    override final def decode(data: Array[Byte]): DecodingResult[Double] = {
      LongBinaryCodec
        .decode(data)
        .right
        .map(l => JvmDouble.longBitsToDouble(l))
    }
  }

  implicit final object FloatBinaryCodec extends Codec[Float] {
    import java.lang.{Float => JvmFloat}
    override final def encode(value: Float): Array[Byte] = {
      val i = JvmFloat.floatToIntBits(value)
      IntBinaryCodec.encode(i)
    }

    override final def decode(data: Array[Byte]): DecodingResult[Float] = {
      IntBinaryCodec
        .decode(data)
        .right
        .map(i => JvmFloat.intBitsToFloat(i))
    }
  }

  implicit final object LongBinaryCodec extends Codec[Long] {
    override final def encode(value: Long): Array[Byte] =
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

    override final def decode(data: Array[Byte]): DecodingResult[Long] = tryDecode(
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

  implicit final object BooleanBinaryCodec extends Codec[Boolean] {
    override final def encode(value: Boolean): Array[Byte] =
      Array((if (value) 1 else 0).asInstanceOf[Byte])

    override final def decode(data: Array[Byte]): DecodingResult[Boolean] =
      tryDecode(data.isDefinedAt(0) && data(0) == 1)
  }

  implicit final object CharBinaryCodec extends Codec[Char] {
    override final def encode(value: Char): Array[Byte] = Array(
      (value >>> 8).asInstanceOf[Byte],
      value.asInstanceOf[Byte]
    )

    override final def decode(data: Array[Byte]): DecodingResult[Char] = tryDecode(
      ((data(0).asInstanceOf[Int] & 255) << 8 |
        data(1).asInstanceOf[Int] & 255)
        .asInstanceOf[Char]
    )
  }

  implicit final object ShortBinaryCodec extends Codec[Short] {
    override final def encode(value: Short): Array[Byte] = Array(
      (value >>> 8).asInstanceOf[Byte],
      value.asInstanceOf[Byte]
    )

    override final def decode(data: Array[Byte]): DecodingResult[Short] = tryDecode(
      ((data(0).asInstanceOf[Short] & 255) << 8 |
        data(1).asInstanceOf[Short] & 255)
        .asInstanceOf[Short]
    )
  }

  implicit final object StringBinaryCodec extends Codec[String] {
    override final def encode(value: String): Array[Byte] = value.getBytes(StandardCharsets.UTF_8)
    override final def decode(data: Array[Byte]): DecodingResult[String] =
      tryDecode(new String(data, StandardCharsets.UTF_8))
  }

  implicit final object ArrayByteBinaryCodec extends Codec[Array[Byte]] {
    override final def encode(value: Array[Byte]): Array[Byte] = value
    override final def decode(data: Array[Byte]): DecodingResult[Array[Byte]] = Right(data)
  }

}

trait LowerPriorityBinaryAnyRefCodecs {

  /**
    * String and Array[Byte] extend java.io.Serializable,
    * so this implicit needs to be lower priority than those in BinaryPrimitiveCodecs
    */
  implicit final def anyRefBinaryCodec[S <: java.io.Serializable](implicit ev: ClassTag[S]): Codec[S] =
    new JavaSerializationAnyRefCodec[S](ev)

}
