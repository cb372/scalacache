package scalacache.serialization

import org.scalatest.{Matchers, FlatSpec}

class JavaSerializationCodecSpec extends FlatSpec with Matchers {

  it should "serialize and deserialize case classes" in {
    val hello = Phone(1, "Apple")
    val phoneCodec = implicitly[Codec[Phone]]
    val serialised = phoneCodec.encode(hello)
    phoneCodec.decode(serialised) shouldBe hello
  }

}

case class Phone(id: Long, manufacturer: String)
