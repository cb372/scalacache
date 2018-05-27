package scalacache.serialization.binary

import scalacache.serialization.Codec
import scalacache.serialization.Codec._
import scodec.bits.ByteVector

import scala.reflect.ClassTag

/**
  * Codecs for all the Java primitive types, plus String and Array[Byte]
  */
trait BinaryPrimitiveCodecs extends LowPriorityBinaryPrimitiveCodecs {

  implicit object IntBinaryCodec extends Codec[Int] {
    override def encode(value: Int): ByteVector = ByteVector.fromInt(value)
    override def decode(data: ByteVector): DecodingResult[Int] = tryDecode(data.toInt())
  }

}

trait LowPriorityBinaryPrimitiveCodecs extends LowerPriorityBinaryAnyRefCodecs {

  implicit object DoubleBinaryCodec extends Codec[Double] {
    import java.lang.{Double => JvmDouble}
    override def encode(value: Double): ByteVector = ByteVector.fromLong(JvmDouble.doubleToLongBits(value))
    override def decode(data: ByteVector): DecodingResult[Double] = tryDecode(JvmDouble.longBitsToDouble(data.toLong()))
  }

  implicit object FloatBinaryCodec extends Codec[Float] {
    import java.lang.{Float => JvmFloat}
    override def encode(value: Float): ByteVector = ByteVector.fromInt(JvmFloat.floatToIntBits(value))
    override def decode(data: ByteVector): DecodingResult[Float] = tryDecode(JvmFloat.intBitsToFloat(data.toInt()))
  }

  implicit object LongBinaryCodec extends Codec[Long] {
    override def encode(value: Long): ByteVector = ByteVector.fromLong(value)
    override def decode(data: ByteVector): DecodingResult[Long] = tryDecode(data.toLong())
  }

  implicit object BooleanBinaryCodec extends Codec[Boolean] {
    override def encode(value: Boolean): ByteVector = ByteVector.fromByte(if (value) 1 else 0)
    override def decode(data: ByteVector): DecodingResult[Boolean] = tryDecode(data(0) == 1)
  }

  implicit object CharBinaryCodec extends Codec[Char] {
    override def encode(value: Char): ByteVector = ByteVector.fromShort(value.asInstanceOf[Short])
    override def decode(data: ByteVector): DecodingResult[Char] = tryDecode(data.toShort().asInstanceOf[Char])
  }

  implicit object ShortBinaryCodec extends Codec[Short] {
    override def encode(value: Short): ByteVector = ByteVector.fromShort(value)
    override def decode(data: ByteVector): DecodingResult[Short] = tryDecode(data.toShort())
  }

  implicit object StringBinaryCodec extends Codec[String] {
    override def encode(value: String): ByteVector = ByteVector.encodeUtf8(value).right.get
    override def decode(data: ByteVector): DecodingResult[String] = tryDecode(data.decodeUtf8)
  }

  implicit object ArrayByteBinaryCodec extends Codec[Array[Byte]] {
    override def encode(value: Array[Byte]): ByteVector = ByteVector(value)
    override def decode(data: ByteVector): DecodingResult[Array[Byte]] = Right(data.toArray)
  }
}

trait LowerPriorityBinaryAnyRefCodecs {

  /**
    * String and Array[Byte] extend java.io.Serializable,
    * so this implicit needs to be lower priority than those in BinaryPrimitiveCodecs
    */
  implicit def anyRefBinaryCodec[S <: java.io.Serializable](implicit ev: ClassTag[S]): Codec[S] =
    new JavaSerializationAnyRefCodec[S](ev)

}
