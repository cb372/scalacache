package scalacache.benchmark

import java.util.concurrent.TimeUnit

import org.cache2k.Cache2kBuilder
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import scalacache._
import scalacache.cache2k.Cache2kCache
import scalacache.memoization._
import scalacache.modes.sync._

@State(Scope.Thread)
class Cache2kBenchmark {

  val underlyingCache = new Cache2kBuilder[String, Entry[String]]() {}.build
  implicit val cache: Cache[String] = Cache2kCache(underlyingCache)

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
  def cache2kGet(bh: Blackhole) = {
    bh.consume(underlyingCache.peek(key))
  }

}
