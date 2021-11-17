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

import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, Inside}
import scalacache._
import scalacache.serialization.Codec.DecodingResult
import scalacache.serialization.binary._
import scalacache.serialization.FailedToDecode

import cats.effect.unsafe.implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import cats.effect.IO
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

trait RedisCacheSpecBase
    extends AnyFlatSpec
    with Matchers
    with Eventually
    with Inside
    with BeforeAndAfter
    with RedisSerialization
    with ScalaFutures
    with IntegrationPatience {

  type JPool
  type JClient <: BaseJedisClient

  case object AlwaysFailing
  implicit val alwaysFailingCodec: BinaryCodec[AlwaysFailing.type] = new BinaryCodec[AlwaysFailing.type] {
    override def encode(value: AlwaysFailing.type): Array[Byte] = Array(0)
    override def decode(bytes: Array[Byte]): DecodingResult[AlwaysFailing.type] =
      Left(FailedToDecode(new Exception("Failed to decode")))
  }

  def withJedis: ((JPool, JClient) => Unit) => Unit
  def constructCache[V](pool: JPool)(implicit codec: BinaryCodec[V]): Cache[IO, String, V]
  def flushRedis(client: JClient): Unit

  def runTestsIfPossible() = {

    withJedis { (pool, client) =>
      val cache        = constructCache[Int](pool)
      val failingCache = constructCache[AlwaysFailing.type](pool)

      before {
        flushRedis(client)
      }

      behavior of "get"

      it should "return the value stored in Redis" in {
        client.set(bytes("key1"), serialize(123))
        whenReady(cache.get("key1").unsafeToFuture()) { _ should be(Some(123)) }
      }

      it should "return None if the given key does not exist in the underlying cache" in {
        whenReady(cache.get("non-existent-key").unsafeToFuture()) { _ should be(None) }
      }

      it should "raise an error if decoding fails" in {
        client.set(bytes("key1"), serialize(123))
        whenReady(failingCache.get("key1").unsafeToFuture().failed) { t =>
          inside(t) { case FailedToDecode(e) => e.getMessage should be("Failed to decode") }
        }
      }

      behavior of "put"

      it should "store the given key-value pair in the underlying cache" in {
        whenReady(cache.put("key2")(123, None).unsafeToFuture()) { _ =>
          deserialize[Int](client.get(bytes("key2"))) should be(Right(123))
        }
      }

      behavior of "put with TTL"

      it should "store the given key-value pair in the underlying cache" in {
        whenReady(cache.put("key3")(123, Some(1 second)).unsafeToFuture()) { _ =>
          deserialize[Int](client.get(bytes("key3"))) should be(Right(123))

          // Should expire after 1 second
          eventually(timeout(Span(2, Seconds))) {
            client.get(bytes("key3")) should be(null)
          }
        }
      }

      behavior of "put with TTL of zero"

      it should "store the given key-value pair in the underlying cache with no expiry" in {
        whenReady(cache.put("key4")(123, Some(Duration.Zero)).unsafeToFuture()) { _ =>
          deserialize[Int](client.get(bytes("key4"))) should be(Right(123))
          client.ttl(bytes("key4")) should be(-1L)
        }
      }

      behavior of "put with TTL of less than 1 second"

      it should "store the given key-value pair in the underlying cache" in {
        whenReady(cache.put("key5")(123, Some(100 milliseconds)).unsafeToFuture()) { _ =>
          deserialize[Int](client.get(bytes("key5"))) should be(Right(123))
          client.pttl("key5").toLong should be > 0L

          // Should expire after 1 second
          eventually(timeout(Span(2, Seconds))) {
            client.get(bytes("key5")) should be(null)
          }
        }
      }

      behavior of "caching with serialization"

      def roundTrip[V](key: String, value: V)(implicit codec: BinaryCodec[V]): Future[Option[V]] = {
        val c = constructCache[V](pool)
        c.put(key)(value, None).flatMap(_ => c.get(key)).unsafeToFuture()
      }

      it should "round-trip a String" in {
        whenReady(roundTrip("string", "hello")) { _ should be(Some("hello")) }
      }

      it should "round-trip a byte array" in {
        whenReady(roundTrip("bytearray", bytes("world"))) { result =>
          new String(result.get, "UTF-8") should be("world")
        }
      }

      it should "round-trip an Int" in {
        whenReady(roundTrip("int", 345)) { _ should be(Some(345)) }
      }

      it should "round-trip a Double" in {
        whenReady(roundTrip("double", 1.23)) { _ should be(Some(1.23)) }
      }

      it should "round-trip a Long" in {
        whenReady(roundTrip("long", 3456L)) { _ should be(Some(3456L)) }
      }

      it should "round-trip a Serializable case class" in {
        val cc = CaseClass(123, "wow")
        whenReady(roundTrip("caseclass", cc)) { _ should be(Some(cc)) }
      }

      behavior of "remove"

      it should "delete the given key and its value from the underlying cache" in {
        client.set(bytes("key1"), serialize(123))
        deserialize[Int](client.get(bytes("key1"))) should be(Right(123))

        whenReady(cache.remove("key1").unsafeToFuture()) { _ =>
          client.get("key1") should be(null)
        }
      }

    }

  }

  def bytes(s: String) = s.getBytes("utf-8")

}
