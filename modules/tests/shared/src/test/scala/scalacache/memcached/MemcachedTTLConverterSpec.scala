package scalacache.memcached

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneOffset}

import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

class MemcachedTTLConverterSpec extends FlatSpec with Matchers with MemcachedTTLConverter {
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
    val now = Instant.now()
    val clock = Clock.fixed(now, ZoneOffset.UTC)
    toMemcachedExpiry(Some(31.days))(clock) should be(now.plus(31, ChronoUnit.DAYS).toEpochMilli / 1000)
  }

}
