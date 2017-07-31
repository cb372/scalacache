package scalacache.memcached

import org.scalatest.{BeforeAndAfter, Matchers, FlatSpec}
import scala.concurrent.duration._
import org.joda.time.{DateTimeZone, DateTime, DateTimeUtils}

class MemcachedTTLConverterSpec
    extends FlatSpec
    with Matchers
    with BeforeAndAfter
    with MemcachedTTLConverter {
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
    val now = DateTime.now(DateTimeZone.UTC)
    DateTimeUtils.setCurrentMillisFixed(now.getMillis)
    toMemcachedExpiry(Some(31.days)) should be(
      now.plusDays(31).getMillis / 1000)
  }

  after {
    DateTimeUtils.setCurrentMillisSystem()
  }

}
