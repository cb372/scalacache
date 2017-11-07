package scalacache.memoization

import scala.concurrent.Future
import scalacache._
import scala.concurrent.ExecutionContext.Implicits.global
import scalacache.modes.scalaFuture._

package object pkg {
  implicit var cache: Cache[Int] = null

  def insidePackageObject(a: Int): Future[Int] = memoize(None) {
    123
  }

}
