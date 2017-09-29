package scalacache.redis

import org.scalatest.{FlatSpec, Matchers}

class RedisSerializationSpec extends FlatSpec with Matchers with RedisSerialization {

  behavior of "serialization"

  it should "round-trip a String" in {
    val bytes = serialize("hello")
    deserialize[String](bytes) should be("hello")
  }

  it should "round-trip a byte array" in {
    val bytes = serialize("world".getBytes("UTF-8"))
    new String(deserialize[Array[Byte]](bytes), "UTF-8") should be("world")
  }

  it should "round-trip an Int" in {
    val bytes = serialize(345)
    deserialize[Int](bytes) should be(345)
  }

  it should "round-trip a Double" in {
    val bytes = serialize(1.23)
    deserialize[Double](bytes) should be(1.23)
  }

  it should "round-trip a Long" in {
    val bytes = serialize(3456L)
    deserialize[Long](bytes) should be(3456L)
  }

  it should "round-trip a Serializable case class" in {
    val cc = CaseClass(123, "wow")
    val bytes = serialize(cc)
    deserialize[CaseClass](bytes) should be(cc)
  }

}
