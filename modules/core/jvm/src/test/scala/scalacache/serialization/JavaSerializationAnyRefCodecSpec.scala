package scalacache.serialization

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JavaSerializationAnyRefCodecSpec extends AnyFlatSpec with Matchers {

  import scalacache.serialization.binary._

  it should "serialize and deserialize case classes" in {
    val hello      = Phone(1, "Apple")
    val phoneCodec = implicitly[Codec[Phone]]
    val serialised = phoneCodec.encode(hello)
    phoneCodec.decode(serialised) shouldBe Right(hello)
  }

  it should "return a Left if the bytes cannot be deserialised to a case class" in {
    val invalidBytes = Array[Byte](1, 2, 3, 4)
    val phoneCodec   = implicitly[Codec[Phone]]
    phoneCodec.decode(invalidBytes) shouldBe 'left
  }

}

case class Phone(id: Long, manufacturer: String)
