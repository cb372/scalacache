/*
 * Copyright 2021 scalacache
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package scalacache.serialization

import io.circe.Json
import org.scalacheck.Arbitrary
import io.circe.syntax._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import scalacache.serialization.binary.BinaryCodec
import org.scalatest.compatible.Assertion

case class Fruit(name: String, tastinessQuotient: Double)

class CirceCodecSpec extends AnyFlatSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  behavior of "JSON serialization using circe"

  import scalacache.serialization.circe._

  private def serdesCheck[A: Arbitrary](expectedJson: A => String)(implicit codec: BinaryCodec[A]): Assertion = {
    forAll(minSuccessful(10000)) { (a: A) =>
      val serialised = codec.encode(a)
      new String(serialised, "utf-8") shouldBe expectedJson(a)
      val deserialised = codec.decode(serialised)
      deserialised.toOption.get shouldBe a
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
    val fruitCodec = implicitly[BinaryCodec[Fruit]]

    val banana     = Fruit("banana", 0.7)
    val serialised = fruitCodec.encode(banana)
    new String(serialised, "utf-8") shouldBe """{"name":"banana","tastinessQuotient":0.7}"""
    val deserialised = fruitCodec.decode(serialised)
    deserialised.toOption.get shouldBe banana
  }

}
