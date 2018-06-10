package scalacache.memoization

import cats.effect.IO
import scalacache._

package object pkg {

  import scalacache.serialization.binary._

  implicit var cache: Cache[IO] = null

  def insidePackageObject(a: Int): IO[Int] = memoize(None) {
    123
  }

}
