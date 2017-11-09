package scalacache.redis

import java.nio.charset.StandardCharsets

import org.scalatest.{FlatSpec, Matchers}

class RedisSerializationSpec extends FlatSpec with Matchers with RedisSerialization {

  behavior of "serialization"

  import scalacache.serialization.binary._

  it should "round-trip a String" in {
    val bytes = serialize("hello")
    deserialize[String](bytes) should be(Right("hello"))
  }

  it should "round-trip a byte array" in {
    val bytes = serialize("world".getBytes("UTF-8"))
    deserialize[Array[Byte]](bytes).right.map(new String(_, StandardCharsets.UTF_8)) should be(Right("world"))
  }

  it should "round-trip an Int" in {
    val bytes = serialize(345)
    deserialize[Int](bytes) should be(Right(345))
  }

  it should "round-trip a Double" in {
    val bytes = serialize(1.23)
    deserialize[Double](bytes) should be(Right(1.23))
  }

  it should "round-trip a Long" in {
    val bytes = serialize(3456L)
    deserialize[Long](bytes) should be(Right(3456L))
  }

  it should "round-trip a Serializable case class" in {
    val cc = CaseClass(123, "wow")
    val bytes = serialize(cc)
    deserialize[CaseClass](bytes) should be(Right(cc))
  }

}
