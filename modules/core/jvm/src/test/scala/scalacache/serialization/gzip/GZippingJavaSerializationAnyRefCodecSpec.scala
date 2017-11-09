package scalacache.serialization.gzip

import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random
import scalacache.serialization.{Codec, Phone}

class GZippingJavaSerializationAnyRefCodecSpec extends FlatSpec with Matchers {

  import GZippingJavaSerializationAnyRefCodec._

  val codec = implicitly[Codec[Phone]]

  it should "work without compression" in {
    val phone = Phone(1, "abc")
    val serialised = codec.encode(phone)
    serialised.head shouldBe CompressingCodec.Headers.Uncompressed
    val deserialised = codec.decode(serialised)
    deserialised shouldBe Right(phone)
  }

  it should "work with compression" in {
    val phone = Phone(1,
                      Random.alphanumeric
                        .take(CompressingCodec.DefaultSizeThreshold + 1)
                        .mkString)
    val serialised = codec.encode(phone)
    serialised.head shouldBe CompressingCodec.Headers.Gzipped
    val deserialised = codec.decode(serialised)
    deserialised shouldBe Right(phone)
  }

}
