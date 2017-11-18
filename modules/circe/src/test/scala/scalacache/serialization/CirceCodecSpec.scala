package scalacache.serialization

import io.circe.{Decoder, Encoder, Json, ObjectEncoder}
import org.scalacheck.Arbitrary
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}
import io.circe.syntax._

case class Fruit(name: String, tastinessQuotient: Double)

class CirceCodecSpec extends FlatSpec with Matchers with GeneratorDrivenPropertyChecks {

  behavior of "JSON serialization using circe"

  import scalacache.serialization.circe._

  private def serdesCheck[A: Arbitrary](expectedJson: A => String)(implicit codec: Codec[A]): Unit = {
    forAll(minSuccessful(10000)) { a: A =>
      val serialised = codec.encode(a)
      new String(serialised, "utf-8") shouldBe expectedJson(a)
      val deserialised = codec.decode(serialised)
      deserialised.right.get shouldBe a
    }
  }

  it should "serialize and deserialize Ints" in {
    serdesCheck[Int](x => s"$x")
  }

  it should "serialize and deserialize Longs" in {
    serdesCheck[Long](x => s"$x")
  }

  it should "serialize and deserialize Doubles" in {
    serdesCheck[Double](x => s"$x")
  }

  it should "serialize and deserialize Floats" in {
    serdesCheck[Float](x => s"$x")
  }

  it should "serialize and deserialize Booleans" in {
    serdesCheck[Boolean](x => s"$x")
  }

  it should "serialize and deserialize Char" in {
    serdesCheck[Char](x => x.asJson.toString)
  }

  it should "serialize and deserialize Short" in {
    serdesCheck[Short](x => s"$x")
  }

  it should "serialize and deserialize String" in {
    serdesCheck[String](x => Json.fromString(x).noSpaces)
  }

  it should "serialize and deserialize Array[Byte]" in {
    serdesCheck[Array[Byte]](x => x.mkString("[", ",", "]"))
  }

  it should "serialize and deserialize a case class" in {
    import io.circe.generic.auto._
    val fruitCodec = implicitly[Codec[Fruit]]

    val banana = Fruit("banana", 0.7)
    val serialised = fruitCodec.encode(banana)
    new String(serialised, "utf-8") shouldBe """{"name":"banana","tastinessQuotient":0.7}"""
    val deserialised = fruitCodec.decode(serialised)
    deserialised.right.get shouldBe banana
  }

}
