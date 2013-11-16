package cacheable.redis

import com.redis._
import cacheable.Cache
import scala.concurrent.duration._
import com.typesafe.scalalogging.slf4j.Logging
import com.redis.serialization.{Parse, Format}
import java.io.{ObjectInputStream, ByteArrayInputStream, ByteArrayOutputStream, ObjectOutputStream}

/**
 * Author: chris
 * Created: 11/16/13
 */
class RedisCache(client: RedisClient) extends Cache with Logging {

  import RedisCache.Serialization._

  /**
   * Get the value corresponding to the given key from the cache
   * @param key cache key
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  def get[V](key: String): Option[V] = {
//    val parser = implicitly[Parse[Int]]
//    val result: Option[Int] = client.get[Int](key)(format = format, parse = parser)
//    result.map(_.asInstanceOf[V])
    val result: Option[V] = client.get[V](key)
    result
  }

  def _get[V](key: String)(implicit manifest: Manifest[V]): Option[V] = {
    //    val parser = implicitly[Parse[Int]]
    //    val result: Option[Int] = client.get[Int](key)(format = format, parse = parser)
    //    result.map(_.asInstanceOf[V])

    val parser = implicitly[Parse[V]]
    val result: Option[V] = client.get(key)(format, parser)
    result
  }

  /**
   * Insert the given key-value pair into the cache, with an optional Time To Live.
   * @param key cache key
   * @param value corresponding value
   * @param ttl Time To Live
   * @tparam V the type of the corresponding value
   */
  def put[V](key: String, value: V, ttl: Option[Duration]): Unit = ttl match {
    case None => client.set(key, value)
    case Some(Duration.Zero) => client.set(key, value)
    case Some(d) if d < 1.second => {
      logger.warn(s"Because Redis does not support sub-second expiry, TTL of $d will be rounded up to 1 second")
      client.setex(key, d.toSeconds.toInt, value)
    }
    case Some(d) => client.setex(key, d.toSeconds.toInt, value)
  }

}

object RedisCache {

  /**
   * Create a Redis client connecting to the given host(s) and use it for caching
   */
  def apply(host: String, port: Int): RedisCache = apply(new RedisClient(host, port))

  /**
   * Create a cache that uses the given Redis client
   * @param client a Redis client
   */
  def apply(client: RedisClient): RedisCache = new RedisCache(client)

  object Serialization {
    /*
     * TODO find a nice way to allow optional customization of serialization.
     * Just use Java serialization for now.
     */

    implicit val format: Format = Format {
      case s: String => { /*println(s"Formatting String $s");*/ s.getBytes("UTF-8") }
      case bs: Array[Byte] => { /*println(s"Formatting Byte array $bs");*/ bs }
      case i: Int => { /*println(s"Formatting Int $i");*/ i.toString.getBytes("UTF-8") }
      case d: Double => d.toString.getBytes("UTF-8")
      case l: Long => l.toString.getBytes("UTF-8")
      case any => {
        println(s"Formatting Any $any")
        // Use Java serialization
        val baos = new ByteArrayOutputStream
        val oos = new ObjectOutputStream(baos)
        oos.writeObject(any)
        baos.toByteArray
      }
    }

    implicit val parseString: Parse[String] = Parse[String]{ bs => { println("Parsing String"); new String(bs) } }
    implicit val parseByteArray: Parse[Array[Byte]] = Parse.Implicits.parseByteArray
    implicit def parseInt: Parse[Int] = Parse[Int]{ bs => { println("Parsing Int"); new String(bs).toInt } }
    implicit val parseLong: Parse[Long] = Parse.Implicits.parseLong
    implicit val parseDouble: Parse[Double] = Parse.Implicits.parseDouble
    implicit def parseJavaSerializedObject[A <: AnyRef]: Parse[A] = Parse { bytes =>
      println("Parsing Any: " + new String(bytes))
      // Anything other than the above uses Java serialization
      val bais = new ByteArrayInputStream(bytes)
      val ois = new ObjectInputStream(bais)
      val result = ois.readObject.asInstanceOf[A]
      println(s"Result: $result")
      result
    }
    implicit def parseAny[A]: Parse[A] = Parse[A] { throw new RuntimeException }

  }

}

