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

package sample

import scalacache._
import memoization._

import scala.concurrent.duration._

import cats.effect.IO

case class User(id: Int, name: String)

/** Sample showing how to use ScalaCache.
  */
object Sample extends App {

  class UserRepository {
    implicit val cache: Cache[IO, String, User] = new MockCache()

    def getUser(id: Int): IO[User] = memoizeF(None) {
      // Do DB lookup here...
      IO { User(id, s"user$id") }
    }

    def withExpiry(id: Int): IO[User] = memoizeF(Some(60 seconds)) {
      // Do DB lookup here...
      IO { User(id, s"user$id") }
    }

    def withOptionalExpiry(id: Int): IO[User] = memoizeF(Some(60 seconds)) {
      IO { User(id, s"user$id") }
    }

    def withOptionalExpiryNone(id: Int): IO[User] = memoizeF(None) {
      IO { User(id, s"user$id") }
    }

  }

}
