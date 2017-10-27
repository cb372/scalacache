package integrationtests

import java.util.UUID

import org.scalatest._
import cats.effect.{IO => CatsIO}
import monix.eval.{Task => MonixTask}
import monix.execution.Scheduler.Implicits.global

import scalaz.concurrent.{Task => ScalazTask}
import net.spy.memcached.{AddrUtil, MemcachedClient}

import scala.language.higherKinds
import scalacache._
import scalacache.caffeine.CaffeineCache
import scalacache.memcached.MemcachedCache

class IntegrationTests extends FlatSpec with Matchers with BeforeAndAfterAll {

//  trait Effect {
//    type F[_]
//    def name: String
//    def mode: Mode[F]
//    def unsafeRunSync[V](fv: F[V]): V
//  }
//
//  private val catsEffectIO: Effect = new Effect {
//    type F[A] = CatsIO[A]
//    val name: String = "cats-effect IO"
//    val mode: Mode[CatsIO] = scalacache.cats.effect.modes.io
//    def unsafeRunSync[V](io: CatsIO[V]): V = io.unsafeRunSync()
//  }
//
//  private val monixTask: Effect = new Effect {
//    type F[A] = MonixTask[A]
//    val name: String = "Monix Task"
//    val mode: Mode[MonixTask] = scalacache.monix.modes.task
//    def unsafeRunSync[V](task: MonixTask[V]): V = task.runSyncMaybe.right.get
//  }
//
//  private val scalazTask: Effect = new Effect {
//    type F[A] = ScalazTask[A]
//    val name: String = "Scalaz Task"
//    val mode: Mode[ScalazTask] = scalacache.scalaz72.modes.task
//    def unsafeRunSync[V](task: ScalazTask[V]): V = task.unsafePerformSync
//  }
//
//  private val memcachedClient = new MemcachedClient(AddrUtil.getAddresses("localhost:11211"))
//
//  override def afterAll(): Unit = {
//    memcachedClient.shutdown()
//  }
//
//  def memcachedIsRunning: Boolean = {
//    try {
//      memcachedClient.get("foo")
//      true
//    } catch { case _: Exception => false }
//  }
//
//  case class CacheBackend(name: String, cache: Cache[String])
//
//  private val caffeine = CacheBackend("caffeine", CaffeineCache[String])
//  private val memcached: Option[CacheBackend] =
//    if (memcachedIsRunning)
//      Some(CacheBackend("memcached", MemcachedCache[String](memcachedClient)))
//    else
//      None
//
//  val backends: List[CacheBackend] = List(Some(caffeine), memcached).flatten
//  val effects: List[Effect] = List(catsEffectIO, monixTask, scalazTask)
//
//  for {
//    CacheBackend(name, cache) <- backends
//    effect <- effects
//  } {
//
//    behavior of s"($name) ⇔ (${effect.name})"
//
//    it should "defer the computation" in {
//      implicit val theCache: Cache[String] = cache
//      implicit val mode: Mode[effect.F] = effect.mode
//
//      val key = UUID.randomUUID().toString
//      val initialValue = UUID.randomUUID().toString
//
//      val program =
//        for {
//          _ <- put(key)(initialValue)
//          readFromCache <- get(key)
//          updatedValue = "prepended " + readFromCache.getOrElse("couldn't find in cache!")
//          _ <- put(key)(updatedValue)
//          finalValueFromCache <- get(key)
//        } yield finalValueFromCache
//
//      assert(finalValueFromCache == "prepended " + initialValue)
//    }
//
//  }

}
