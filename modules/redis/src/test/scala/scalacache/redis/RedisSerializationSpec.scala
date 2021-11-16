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

package scalacache.redis

import java.nio.charset.StandardCharsets

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RedisSerializationSpec extends AnyFlatSpec with Matchers with RedisSerialization {

  behavior of "serialization"

  import scalacache.serialization.binary._

  it should "round-trip a String" in {
    val bytes = serialize("hello")
    deserialize[String](bytes) should be(Right("hello"))
  }

  it should "round-trip a byte array" in {
    val bytes = serialize("world".getBytes("UTF-8"))
    deserialize[Array[Byte]](bytes).map(new String(_, StandardCharsets.UTF_8)) should be(Right("world"))
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
    val cc    = CaseClass(123, "wow")
    val bytes = serialize(cc)
    deserialize[CaseClass](bytes) should be(Right(cc))
  }

}
