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

package integrationtests

import java.util.UUID

import org.scalatest._
import cats.effect.{IO => CatsIO}
import net.spy.memcached.{AddrUtil, MemcachedClient}
import redis.clients.jedis.JedisPool

import scala.util.control.NonFatal
import scalacache._
import scalacache.caffeine.CaffeineCache
import scalacache.memcached.MemcachedCache
import scalacache.redis.RedisCache
import cats.effect.Clock
import cats.effect.unsafe.implicits.global
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class IntegrationTests extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  implicit val catsClock: Clock[CatsIO] = Clock[CatsIO]

  private val memcachedClient = new MemcachedClient(AddrUtil.getAddresses("localhost:11211"))
  private val jedisPool       = new JedisPool("localhost", 6379)

  override def afterAll(): Unit = {
    memcachedClient.shutdown()
    jedisPool.close()
  }

  private def memcachedIsRunning: Boolean = {
    try {
      memcachedClient.get("foo")
      true
    } catch { case _: Exception => false }
  }

  private def redisIsRunning: Boolean = {
    try {
      val jedis = jedisPool.getResource()
      try {
        jedis.ping()
        true
      } finally {
        jedis.close()
      }
    } catch {
      case NonFatal(_) => false
    }
  }

  case class CacheBackend(name: String, cache: Cache[CatsIO, String, String])

  private val caffeine = CacheBackend("Caffeine", CaffeineCache[CatsIO, String, String].unsafeRunSync())
  private val memcached: Seq[CacheBackend] =
    if (memcachedIsRunning) {
      Seq(
        {
          import scalacache.serialization.binary._
          CacheBackend("(Memcached) ⇔ (binary codec)", MemcachedCache[CatsIO, String](memcachedClient))
        }, {
          import scalacache.serialization.circe._
          CacheBackend("(Memcached) ⇔ (circe codec)", MemcachedCache[CatsIO, String](memcachedClient))
        }
      )
    } else {
      alert("Skipping Memcached integration tests because Memcached does not appear to be running on localhost.")
      Nil
    }

  private val redis: Seq[CacheBackend] =
    if (redisIsRunning)
      Seq(
        {
          import scalacache.serialization.binary._
          CacheBackend("(Redis) ⇔ (binary codec)", RedisCache[CatsIO, String, String](jedisPool))
        }, {
          import scalacache.serialization.circe._
          CacheBackend("(Redis) ⇔ (circe codec)", RedisCache[CatsIO, String, String](jedisPool))
        }
      )
    else {
      alert("Skipping Redis integration tests because Redis does not appear to be running on localhost.")
      Nil
    }

  val backends: List[CacheBackend] = List(caffeine) ++ memcached ++ redis

  for (CacheBackend(name, cache) <- backends) {

    s"$name ⇔ (cats-effect IO)" should "defer the computation and give the correct result" in {
      implicit val theCache: Cache[CatsIO, String, String] = cache

      val key          = UUID.randomUUID().toString
      val initialValue = UUID.randomUUID().toString

      val program =
        for {
          _             <- put(key)(initialValue)
          readFromCache <- get(key)
          updatedValue = "prepended " + readFromCache.getOrElse("couldn't find in cache!")
          _                   <- put(key)(updatedValue)
          finalValueFromCache <- get(key)
        } yield finalValueFromCache

      checkComputationHasNotRun(key)

      val result: Option[String] = program.unsafeRunSync()
      assert(result.contains("prepended " + initialValue))
    }
  }

  private def checkComputationHasNotRun(key: String)(implicit cache: Cache[CatsIO, String, String]): Assertion = {
    Thread.sleep(1000)
    assert(cache.get(key).unsafeRunSync().isEmpty)
  }

}
