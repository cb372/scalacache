package common

import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.{ Matchers, FlatSpec }

import scalacache._
import scalacache.serialization.Codec

object Snack {
  val Jagabee: Snack = Snack("Jagabee")
}

case class Snack(name: String)

class DummySnackCodec extends Codec[Snack, Array[Byte]] {
  var serialiserUsed = false
  var deserialiserUsed = false
  def serialize(value: Snack): Array[Byte] = {
    serialiserUsed = true
    Array.empty
  }
  def deserialize(data: Array[Byte]): Snack = {
    deserialiserUsed = true
    Snack.Jagabee
  }
}

trait LegacyCodecCheckSupport { this: FlatSpec with Matchers with ScalaFutures with IntegrationPatience =>

  /**
   * Given a function that returns a [[Cache]] based on whether or not Codec-based serialisation should be skipped,
   * performs a basic check to verify usage/non-usage of in-scope Codecs.
   *
   * @param buildCache function that takes a boolean indicating whether not the cache returned should make use of
   *                   in-scope Codecs
   */
  def legacySupportCheck(buildCache: Boolean => Cache[Array[Byte]]): Unit = {

    behavior of "useLegacySerialization"

    it should "use the in-scope Codec if useLegacySerialization is false" in {
      implicit val codec = new DummySnackCodec
      val cache = buildCache(false)
      whenReady(cache.put("snack", Snack.Jagabee, None)) { _ =>
        codec.serialiserUsed shouldBe true
        whenReady(cache.get[Snack]("snack")) { _ =>
          codec.deserialiserUsed shouldBe true
        }
      }
    }

    it should "use not the in-scope Codec if useLegacySerialization is true" in {
      implicit val codec = new DummySnackCodec
      val cache = buildCache(true)
      whenReady(cache.put("snack", Snack.Jagabee, None)) { _ =>
        codec.serialiserUsed shouldBe false
        whenReady(cache.get[Snack]("snack")) { _ =>
          codec.deserialiserUsed shouldBe false
        }
      }
    }
  }
}