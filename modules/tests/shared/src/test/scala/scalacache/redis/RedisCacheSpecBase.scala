package scalacache.redis

import java.nio.charset.StandardCharsets

import cats.effect.IO
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, FlatSpec, Inside, Matchers}
import redis.clients.jedis.{BinaryJedisCommands, JedisCommands}
import scalacache._
import scalacache.serialization.{Codec, FailedToDecode}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.{higherKinds, postfixOps}

trait RedisCacheSpecBase
    extends FlatSpec
    with Matchers
    with Eventually
    with Inside
    with BeforeAndAfter
    with ScalaFutures
    with IntegrationPatience {

  import scalacache.CatsEffect.implicits._

  def serialize[A](value: A)(implicit codec: Codec[A]): Array[Byte] =
    codec.encode(value)

  def deserialize[A](bytes: Array[Byte])(implicit codec: Codec[A]): Codec.DecodingResult[A] =
    codec.decode(bytes)

  type JPool
  type JClient <: JedisCommands with BinaryJedisCommands

  case object AlwaysFailing
  // TODO Jules: Uncomment or delete ?
  //implicit val alwaysFailingCodec: Codec[AlwaysFailing.type] = new Codec[AlwaysFailing.type] {
  //  override def encode(value: AlwaysFailing.type): Array[Byte] = Array[Byte].empty
  //  override def decode(bytes: Array[Byte]): DecodingResult[AlwaysFailing.type] =
  //    Left(FailedToDecode(new Exception("Failed to decode")))
  //}

  def withJedis: ((JPool, JClient) => Unit) => Unit
  def constructCache[F[_]: Async](pool: JPool): CacheAlg[F]
  def flushRedis(client: JClient): Unit

  def runTestsIfPossible() = {

    withJedis { (pool, client) =>
      val cache = constructCache[IO](pool)
      val failingCache: Cache[IO] = new Cache[IO] {

        override type Underlying = Null
        override val underlying: Underlying = null

        override def config: CacheConfig = null

        override def cachingForMemoize[V](baseKey: String)(ttl: Option[Duration])(f: => V)(implicit codec: Codec[V],
                                                                                           flags: Flags): IO[V] =
          IO.raiseError(new Exception("always failing cache"))

        override def cachingForMemoizeF[V](baseKey: String)(ttl: Option[Duration])(
            f: => IO[V])(implicit codec: Codec[V], flags: Flags): IO[V] =
          IO.raiseError(new Exception("always failing cache"))

        override def get[V: Codec](keyParts: Any*)(implicit flags: Flags): IO[Option[V]] =
          IO.raiseError(new Exception("always failing cache"))

        override def put[V: Codec](keyParts: Any*)(value: V, ttl: Option[Duration])(implicit flags: Flags): IO[Unit] =
          IO.raiseError(new Exception("always failing cache"))

        override def remove(keyParts: Any*): IO[Unit] = IO.raiseError(new Exception("always failing cache"))
        override def removeAll(): IO[Unit] = IO.raiseError(new Exception("always failing cache"))
        override def caching[V: Codec](keyParts: Any*)(ttl: Option[Duration])(f: => V)(implicit flags: Flags): IO[V] =
          IO.raiseError(new Exception("always failing cache"))
        override def cachingF[V: Codec](keyParts: Any*)(ttl: Option[Duration])(f: => IO[V])(
            implicit flags: Flags): IO[V] = IO.raiseError(new Exception("always failing cache"))
        override def close(): IO[Unit] = IO.raiseError(new Exception("always failing cache"))
      }

      before {
        flushRedis(client)
      }

      behavior of "get"

      it should "return the value stored in Redis" in {
        import scalacache.serialization.binary._

        client.set(bytes("key1"), serialize(123))
        cache.get[Int]("key1").unsafeRunSync() should be(Some(123))
      }

      it should "return None if the given key does not exist in the underlying cache" in {
        import scalacache.serialization.binary._

        whenReady(cache.get[Int]("non-existent-key").unsafeToFuture()) { _ should be(None) }
      }

      it should "raise an error if decoding fails" in {
        import scalacache.serialization.binary._

        client.set(bytes("key1"), serialize(123))
        whenReady(failingCache.get[Int]("key1").unsafeToFuture().failed) { t =>
          inside(t) { case FailedToDecode(e) => e.getMessage should be("Failed to decode") }
        }
      }

      behavior of "put"

      it should "store the given key-value pair in the underlying cache" in {
        import scalacache.serialization.binary._

        whenReady(cache.put("key2")(123, None).unsafeToFuture()) { _ =>
          deserialize[Int](client.get(bytes("key2"))) should be(Right(123))
        }
      }

      behavior of "put with TTL"

      it should "store the given key-value pair in the underlying cache" in {
        import scalacache.serialization.binary._

        whenReady(cache.put("key3")(123, Some(1 second)).unsafeToFuture()) { _ =>
          deserialize[Int](client.get(bytes("key3"))) should be(Right(123))

          // Should expire after 1 second
          eventually(timeout(Span(2, Seconds))) {
            client.get(bytes("key3")) should be(null)
          }
        }
      }

      behavior of "put with TTL of zero"

      it should "store the given key-value pair in the underlying cache with no expiry" in {
        import scalacache.serialization.binary._

        whenReady(cache.put("key4")(123, Some(Duration.Zero)).unsafeToFuture()) { _ =>
          deserialize[Int](client.get(bytes("key4"))) should be(Right(123))
          client.ttl(bytes("key4")) should be(-1L)
        }
      }

      behavior of "put with TTL of less than 1 second"

      it should "store the given key-value pair in the underlying cache" in {
        import scalacache.serialization.binary._

        whenReady(cache.put("key5")(123, Some(100 milliseconds)).unsafeToFuture()) { _ =>
          deserialize[Int](client.get(bytes("key5"))) should be(Right(123))
          client.pttl("key5").toLong should be > 0L

          // Should expire after 1 second
          eventually(timeout(Span(2, Seconds))) {
            client.get(bytes("key5")) should be(null)
          }
        }
      }

      behavior of "caching with serialization"

      def roundTrip[V: Codec](key: String, value: V): Future[Option[V]] = {
        val c = constructCache[IO](pool)
        c.put(key)(value, None).flatMap(_ => c.get(key)).unsafeToFuture()
      }

      import scalacache.serialization.binary._

      it should "round-trip a String" in {
        whenReady(roundTrip("string", "hello")) { _ should be(Some("hello")) }
      }

      it should "round-trip a byte array" in {
        whenReady(roundTrip("bytearray", bytes("world"))) { result =>
          new String(result.get, StandardCharsets.UTF_8) should be("world")
        }
      }

      it should "round-trip an Int" in {
        whenReady(roundTrip("int", 345)) { _ should be(Some(345)) }
      }

      it should "round-trip a Double" in {
        whenReady(roundTrip("double", 1.23)) { _ should be(Some(1.23)) }
      }

      it should "round-trip a Long" in {
        whenReady(roundTrip("long", 3456L)) { _ should be(Some(3456L)) }
      }

      it should "round-trip a Serializable case class" in {
        val cc = CaseClass(123, "wow")
        whenReady(roundTrip("caseclass", cc)) { _ should be(Some(cc)) }
      }

      behavior of "remove"

      it should "delete the given key and its value from the underlying cache" in {
        import scalacache.serialization.binary._
        client.set(bytes("key1"), serialize(123))
        deserialize[Int](client.get(bytes("key1"))) should be(Right(123))

        whenReady(cache.remove("key1").unsafeToFuture()) { _ =>
          client.get("key1") should be(null)
        }
      }

    }

  }

  def bytes(s: String): Array[Byte] = s.getBytes(StandardCharsets.UTF_8)

}
