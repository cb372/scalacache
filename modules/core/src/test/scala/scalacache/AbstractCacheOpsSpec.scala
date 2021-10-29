package scalacache

import org.scalatest.BeforeAndAfter

import scala.concurrent.duration._
import scala.language.postfixOps

import scala.util.{Success, Try}
import cats.effect.SyncIO
import cats.syntax.all._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AbstractCacheOpsSpec extends AnyFlatSpec with Matchers with BeforeAndAfter {

  val cache = new LoggingMockCache[SyncIO, String]

  before {
    cache.mmap.clear()
    cache.reset.unsafeRunSync()
  }

  behavior of "#cachingRight"

  it should "run the block and cache its Right result with no TTL" in {
    val effect = cache
      .cachingRight("myKey")(None) {
        "result of block".asRight[Other]
      } *>
      cache
        .cachingRight("myKey")(None) {
          "result of block".asRight[Other]
        }

    val result = effect.unsafeRunSync()

    cache.getCalledWithArgs.length shouldBe 2
    cache.getCalledWithArgs(0) should be("myKey")
    cache.getCalledWithArgs(1) should be("myKey")
    cache.putCalledWithArgs.length shouldBe 1
    cache.putCalledWithArgs(0) should be("myKey", "result of block", None)
    result should be("result of block".asRight[Other])
  }

  it should "run the block and not cache its Left result" in {
    val effect = cache
      .cachingRight("myKey")(None) {
        Other("error").asLeft[String]
      } *>
      cache
        .cachingRight("myKey")(None) {
          "result of block".asRight[Other]
        }

    val result = effect.unsafeRunSync()

    cache.getCalledWithArgs.length shouldBe 2
    cache.getCalledWithArgs(0) should be("myKey")
    cache.getCalledWithArgs(1) should be("myKey")
    cache.putCalledWithArgs.length shouldBe 1
    cache.putCalledWithArgs(0) should be("myKey", "result of block", None)
    result should be("result of block".asRight[Other])
  }

  behavior of "#cachingSome"

  it should "run the block and cache its Some result with no TTL" in {
    val effect = cache
      .cachingSome("myKey")(None) {
        "result of block".some
      } *>
      cache
        .cachingSome("myKey")(None) {
          "result of block".some
        }

    val result = effect.unsafeRunSync()

    cache.getCalledWithArgs.length shouldBe 2
    cache.getCalledWithArgs(0) should be("myKey")
    cache.getCalledWithArgs(1) should be("myKey")
    cache.putCalledWithArgs.length shouldBe 1
    cache.putCalledWithArgs(0) should be("myKey", "result of block", None)
    result should be("result of block".some)
  }

  it should "run the block and not cache its None result" in {
    val effect = cache
      .cachingSome("myKey")(None) {
        None
      } *>
      cache
        .cachingSome("myKey")(None) {
          "result of block".some
        }

    val result = effect.unsafeRunSync()

    cache.getCalledWithArgs.length shouldBe 2
    cache.getCalledWithArgs(0) should be("myKey")
    cache.getCalledWithArgs(1) should be("myKey")
    cache.putCalledWithArgs.length shouldBe 1
    cache.putCalledWithArgs(0) should be("myKey", "result of block", None)
    result should be("result of block".some)
  }

}

case class Other(s: String)
