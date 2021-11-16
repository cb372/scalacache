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

package scalacache.memcached

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneOffset}

import scala.concurrent.duration._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MemcachedTTLConverterSpec extends AnyFlatSpec with Matchers with MemcachedTTLConverter {
  behavior of "MemcachedTTLConverter"

  it should "convert None to 0" in {
    toMemcachedExpiry(None) should be(0)
  }

  it should "convert Some(Duration.Zero) to 0" in {
    toMemcachedExpiry(Some(Duration.Zero)) should be(0)
  }

  it should "round up Some(1.millisecond) to 1 second" in {
    toMemcachedExpiry(Some(1.millisecond)) should be(1)
  }

  it should "convert Some(1.second) to 1 second" in {
    toMemcachedExpiry(Some(1.second)) should be(1)
  }

  it should "convert Some(3.hours) to seconds" in {
    toMemcachedExpiry(Some(3.hours)) should be(3 * 60 * 60)
  }

  it should "convert Some(30.days) to seconds" in {
    toMemcachedExpiry(Some(30.days)) should be(30 * 24 * 60 * 60)
  }

  it should "convert a duration longer than 30 days to the expiry time expressed as UNIX epoch seconds" in {
    val now   = Instant.now()
    val clock = Clock.fixed(now, ZoneOffset.UTC)
    toMemcachedExpiry(Some(31.days))(clock) should be(now.plus(31, ChronoUnit.DAYS).toEpochMilli / 1000)
  }

}
