package scalacache.benchmark

import java.util.concurrent.TimeUnit

import cats.effect.IO
import org.cache2k.Cache2kBuilder
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import scalacache._
import scalacache.cache2k.Cache2kCache
import scalacache.memoization._

import scala.concurrent.duration._

@State(Scope.Thread)
class Cache2kBenchmark {

  val underlyingCache =
    new Cache2kBuilder[String, Array[Byte]]() {}
      .expireAfterWrite(1, DAYS)
      .build

  implicit val cache: Cache[IO] = Cache2kCache[IO](underlyingCache)

  val key = "key"
  val value: String = "value"

  def itemCachedNoMemoize(key: String): IO[Option[String]] = cache.get(key)

  def itemCachedMemoize(key: String): IO[String] = memoize(None) { value }

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

  @TearDown
  def close(): Unit = cache.close()

}
