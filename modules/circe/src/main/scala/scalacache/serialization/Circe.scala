package scalacache.serialization

import java.nio.ByteBuffer
import io.circe.jawn.JawnParser
import scalacache.serialization.binary.BinaryCodec

package object circe {

  private[this] val parser = new JawnParser

  implicit def codec[A](implicit encoder: io.circe.Encoder[A], decoder: io.circe.Decoder[A]): BinaryCodec[A] =
    new BinaryCodec[A] {

      override def encode(value: A): Array[Byte] = encoder.apply(value).noSpaces.getBytes("utf-8")

      override def decode(bytes: Array[Byte]): Codec.DecodingResult[A] =
        parser.decodeByteBuffer(ByteBuffer.wrap(bytes)).left.map(FailedToDecode)

    }

}
