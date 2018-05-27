package scalacache.memoization

import scalacache._

import scala.concurrent.Future

package object pkg {

  import scalacache.serialization.binary._

  implicit var cache: Cache[Future] = null

  def insidePackageObject(a: Int): Future[Int] = memoize(None) {
    123
  }

}
