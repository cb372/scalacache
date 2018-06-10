package scalacache.benchmark

import java.util.concurrent.TimeUnit

import cats.effect.IO
import com.github.benmanes.caffeine.cache.Caffeine
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import scalacache._
import scalacache.caffeine.CaffeineCache

@State(Scope.Thread)
class CaffeineBenchmark {
  import memoization._

  val underlyingCache = Caffeine.newBuilder().build[String, Entry]()
  implicit val cache: Cache[IO] = CaffeineCache[IO](underlyingCache)

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
  def caffeineGet(bh: Blackhole) = {
    bh.consume(underlyingCache.getIfPresent(key))
  }

}
