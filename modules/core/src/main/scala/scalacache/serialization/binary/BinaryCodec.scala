package scalacache.serialization.binary

import scalacache.serialization.{Codec, Decoder, Encoder}

trait BinaryEncoder[T] extends Encoder[T, Array[Byte]] {
  override def encode(value: T): Array[Byte]
}

trait BinaryDecoder[T] extends Decoder[T, Array[Byte]] {
  override def decode(value: Array[Byte]): Codec.DecodingResult[T]
}

trait BinaryCodec[T] extends Codec[T, Array[Byte]] with BinaryEncoder[T] with BinaryDecoder[T] {
  override def encode(value: T): Array[Byte]
  override def decode(bytes: Array[Byte]): Codec.DecodingResult[T]
}
