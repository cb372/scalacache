package scalacache.benchmark

import java.util.concurrent.TimeUnit

import cats.effect.IO
import org.caffinitas.ohc.{OHCache, OHCacheBuilder}
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import scalacache.memoization._
import scalacache.ohc.OhcCache

@State(Scope.Thread)
class OhcBenchmark {

  val underlyingCache: OHCache[String, Array[Byte]] =
    OHCacheBuilder
      .newBuilder()
      .keySerializer(OhcCache.stringSerializer)
      .valueSerializer(OhcCache.bytesSerializer)
      .timeouts(true)
      .build()

  implicit val cache: OhcCache[IO] = OhcCache[IO](underlyingCache)

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
  def ohcGet(bh: Blackhole) = {
    bh.consume(underlyingCache.get(key))
  }

  @TearDown
  def close(): Unit = cache.close()

}
