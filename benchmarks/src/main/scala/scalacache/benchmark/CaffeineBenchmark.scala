package scalacache.benchmark

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.benmanes.caffeine.cache.Caffeine

import scalacache._
import caffeine._
import memoization._

@State(Scope.Thread)
class CaffeineBenchmark {

  val underlyingCache = Caffeine.newBuilder().build[String, Object]()
  implicit val scalaCache = ScalaCache(CaffeineCache(underlyingCache))
  val typedCache = typed[String, NoSerialization]

  val key = "key"
  val value: String = "value"

  def itemCachedNoMemoize(key: String): Future[Option[String]] = {
    get[String, NoSerialization](key)
  }

  def itemCachedTypedNoMemoize(key: String): Future[Option[String]] = {
    typedCache.get(key)
  }

  def itemCachedMemoize(key: String): Future[String] = memoize {
    Future.successful(value)
  }

  // populate the cache
  put(key)(value)

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  def scalacacheGetNoMemoize(bh: Blackhole) = {
    bh.consume(Await.result(itemCachedNoMemoize(key), Duration.Inf))
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  def scalacacheGetTypedNoMemoize(bh: Blackhole) = {
    bh.consume(Await.result(itemCachedTypedNoMemoize(key), Duration.Inf))
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  def scalacacheGetWithMemoize(bh: Blackhole) = {
    bh.consume(Await.result(itemCachedMemoize(key), Duration.Inf))
  }

  @Benchmark
  @BenchmarkMode(Array(Mode.AverageTime))
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  def caffeineGet(bh: Blackhole) = {
    bh.consume(underlyingCache.getIfPresent(key))
  }

}
