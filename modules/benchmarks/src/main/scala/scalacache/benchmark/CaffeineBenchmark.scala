package scalacache.benchmark

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import com.github.benmanes.caffeine.cache.Caffeine

import scalacache._
import caffeine._
import memoization._
import scalacache.modes.sync._

@State(Scope.Thread)
class CaffeineBenchmark {

  val underlyingCache = Caffeine.newBuilder().build[String, Entry[String]]()
  implicit val cache: Cache[String] = CaffeineCache(underlyingCache)

  val key = "key"
  val value: String = "value"

  def itemCachedNoMemoize(key: String): Id[Option[String]] = {
    cache.get(key)
  }

  def itemCachedMemoize(key: String): String = memoizeSync(None) {
    value
  }

  // populate the cache
  cache.put(key)(value)

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  def scalacacheGetNoMemoize(bh: Blackhole) = {
    bh.consume(itemCachedNoMemoize(key))
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  def scalacacheGetWithMemoize(bh: Blackhole) = {
    bh.consume(itemCachedMemoize(key))
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  def caffeineGet(bh: Blackhole) = {
    bh.consume(underlyingCache.getIfPresent(key))
  }

}
