/*
 * Copyright 2021 scalacache
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package issue42

import scala.util.Random
import cats.effect.SyncIO
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Issue42Spec extends AnyFlatSpec with Matchers {

  case class User(id: Int, name: String)

  import scalacache._
  import memoization._

  import concurrent.duration._

  implicit val cache: Cache[SyncIO, String, User] = new MockCache()

  def generateNewName() = Random.alphanumeric.take(10).mkString

  def getUser(id: Int)(implicit flags: Flags): User =
    memoize(None) {
      User(id, generateNewName())
    }.unsafeRunSync()

  def getUserWithTtl(id: Int)(implicit flags: Flags): User =
    memoize(Some(1 days)) {
      User(id, generateNewName())
    }.unsafeRunSync()

  "memoize without TTL" should "respect implicit flags" in {
    val user1before = getUser(1)
    val user1after = {
      implicit val flags = Flags(readsEnabled = false)
      getUser(1)
    }
    user1before should not be user1after
  }

  "memoize with TTL" should "respect implicit flags" in {
    val user1before = getUserWithTtl(1)
    val user1after = {
      implicit val flags = Flags(readsEnabled = false)
      getUserWithTtl(1)
    }
    user1before should not be user1after
  }

}
