package scalacache.memoization

import scalacache._
import scalacache.modes.sync._

package object pkg {
  implicit var cache: Cache[Int] = null

  def insidePackageObject(a: Int): Int = memoizeSync(None) {
    123
  }

}
