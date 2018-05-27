package scalacache.memoization

import scala.concurrent.Future
import scalacache._
import scala.concurrent.ExecutionContext.Implicits.global
import scalacache.modes.scalaFuture._

package object pkg {

  import scalacache.serialization.binary._

  implicit var cache: Cache[Future] = null

  def insidePackageObject(a: Int): Future[Int] = memoize(None) {
    123
  }

}
