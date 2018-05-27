package scalacache.serialization

import io.circe.jawn.JawnParser
import io.circe.{Decoder, Encoder}
import scodec.bits.ByteVector

package object circe {

  private[this] val parser = new JawnParser

  implicit def codec[A](implicit encoder: Encoder[A], decoder: Decoder[A]): Codec[A] = new Codec[A] {

    override def encode(value: A): ByteVector = ByteVector.encodeUtf8(encoder.apply(value).noSpaces).right.get

    override def decode(bytes: ByteVector): Codec.DecodingResult[A] =
      parser.decodeByteBuffer(bytes.toByteBuffer).left.map(FailedToDecode)

  }

}
