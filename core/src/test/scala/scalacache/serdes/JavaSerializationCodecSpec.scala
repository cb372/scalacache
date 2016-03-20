package scalacache.serdes

import org.scalatest.{ Matchers, FlatSpec }
import JavaSerializationCodecs._

class JavaSerializationCodecSpec extends FlatSpec with Matchers {

  it should "serialize and deserialize case classes" in {
    val hello = Phone(1, "Apple")
    val serialised = implicitly[Codec[Phone]].serialize(hello)
    implicitly[Codec[Phone]].deserialize(serialised) shouldBe hello
  }

}

case class Phone(id: Long, manufacturer: String)