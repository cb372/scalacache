package scalacache.memoization

import scalacache._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 *
 * Author: c-birchall
 * Date:   13/11/07
 */
package object pkg {
  implicit var scalaCache: ScalaCache = null

  def insidePackageObject(a: Int): Int = memoize {
    123
  }

}
