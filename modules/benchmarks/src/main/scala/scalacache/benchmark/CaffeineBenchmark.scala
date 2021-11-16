package scalacache.benchmark

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

import com.github.benmanes.caffeine.cache.Caffeine

import scalacache._
import caffeine._
import memoization._
import cats.effect.SyncIO
import cats.effect.Clock
import scala.annotation.nowarn

@State(Scope.Thread)
class CaffeineBenchmark {

  implicit val clockSyncIO: Clock[SyncIO] = Clock[SyncIO]

  val underlyingCache = Caffeine.newBuilder().build[String, Entry[String]]()
  implicit val cache: Cache[SyncIO, String, String] =
    CaffeineCache[SyncIO, String, String](underlyingCache)

  val key           = "key"
  val value: String = "value"

  def itemCachedNoMemoize(key: String): Option[String] = {
    cache.get(key).unsafeRunSync()
  }

  @nowarn
  def itemCachedMemoize(key: String): String =
    memoize(None) {
      value
    }.unsafeRunSync()

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
