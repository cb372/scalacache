import scalacache._
import redis._
import memoization._

object Main extends App {
  implicit val scalaCache = ScalaCache(RedisCache("localhost", 6379))

  def addOne(a: Int): Int = memoize { a + 1 }

  println(s"5 + 1 = ${addOne(5)}")
}
