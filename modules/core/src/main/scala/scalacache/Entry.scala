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

package scalacache

import java.time.Instant
import cats.effect.Clock
import cats.implicits._
import cats.Applicative

/** A cache entry with an optional expiry time
  */
case class Entry[+A](value: A, expiresAt: Option[Instant])

object Entry {

  def isBeforeExpiration[F[_], A](entry: Entry[A])(implicit clock: Clock[F], applicative: Applicative[F]): F[Boolean] =
    entry.expiresAt
      .traverse { expiration =>
        clock.monotonic.map(m => Instant.ofEpochMilli(m.toMillis).isBefore(expiration))
      }
      .map {
        case None                   => true // no expiration set for entry, never expires
        case Some(beforeExpiration) => beforeExpiration
      }

  def isExpired[F[_], A](entry: Entry[A])(implicit clock: Clock[F], applicative: Applicative[F]): F[Boolean] =
    isBeforeExpiration[F, A](entry).map(b => !b)
}
