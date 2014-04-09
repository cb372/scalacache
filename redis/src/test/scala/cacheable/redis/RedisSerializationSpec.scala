package cacheable.redis

import org.scalatest.{ FlatSpec, ShouldMatchers }

/**
 * Author: chris
 * Created: 11/17/13
 */
class RedisSerializationSpec extends FlatSpec with ShouldMatchers with RedisSerialization {

  behavior of "serialization"

  it should "round-trip a String" in {
    val bytes = format("hello")
    parse[String](bytes) should be("hello")
  }

  it should "round-trip a byte array" in {
    val bytes = format("world".getBytes("UTF-8"))
    new String(parse[Array[Byte]](bytes), "UTF-8") should be("world")
  }

  it should "round-trip an Int" in {
    val bytes = format(345)
    parse[Int](bytes) should be(345)
  }

  it should "round-trip a Double" in {
    val bytes = format(1.23)
    parse[Double](bytes) should be(1.23)
  }

  it should "round-trip a Long" in {
    val bytes = format(3456L)
    parse[Long](bytes) should be(3456L)
  }

  it should "round-trip a Serializable case class" in {
    val cc = CaseClass(123, "wow")
    val bytes = format(cc)
    parse[CaseClass](bytes) should be(cc)
  }

}
