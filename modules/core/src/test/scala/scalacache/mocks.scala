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

import cats.effect.Sync
import scalacache.logging.Logger
import scalacache.memoization.MemoizationConfig

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration
import cats.syntax.functor._

class EmptyCache[F[_], V](implicit val F: Sync[F], val config: MemoizationConfig) extends AbstractCache[F, String, V] {

  override protected def logger = Logger.getLogger("EmptyCache")

  override protected def doGet(key: String) =
    F.pure(None)

  override protected def doPut(key: String, value: V, ttl: Option[Duration]) =
    F.unit

  override protected def doRemove(key: String) =
    F.unit

  override protected val doRemoveAll =
    F.unit

  override val close = F.unit

}

class FullCache[F[_], V](value: V)(implicit val F: Sync[F], val config: MemoizationConfig)
    extends AbstractCache[F, String, V] {

  override protected def logger = Logger.getLogger("FullCache")

  override protected def doGet(key: String) =
    F.pure(Some(value))

  override protected def doPut(key: String, value: V, ttl: Option[Duration]) =
    F.unit

  override protected def doRemove(key: String) =
    F.unit

  override protected val doRemoveAll =
    F.unit

  override val close = F.unit

}

class ErrorRaisingCache[F[_], V](implicit val F: Sync[F], val config: MemoizationConfig)
    extends AbstractCache[F, String, V] {

  override protected val logger = Logger.getLogger("FullCache")

  override protected def doGet(key: String) =
    F.raiseError(new RuntimeException("failed to read"))

  override protected def doPut(key: String, value: V, ttl: Option[Duration]) =
    F.raiseError(new RuntimeException("failed to write"))

  override protected def doRemove(key: String) =
    F.unit

  override protected val doRemoveAll =
    F.unit

  override val close = F.unit

}

/** A mock cache for use in tests and samples. Does not support TTL.
  */
class MockCache[F[_], V](implicit val F: Sync[F], val config: MemoizationConfig) extends AbstractCache[F, String, V] {

  override protected def logger = Logger.getLogger("MockCache")

  val mmap = collection.mutable.Map[String, V]()

  override protected def doGet(key: String) =
    F.delay(mmap.get(key))

  override protected def doPut(key: String, value: V, ttl: Option[Duration]) =
    F.delay(mmap.put(key, value)).void

  override protected def doRemove(key: String) =
    F.delay(mmap.remove(key)).void

  override protected val doRemoveAll =
    F.delay(mmap.clear())

  override val close = F.unit

}

/** A cache that keeps track of the arguments it was called with. Useful for tests. Designed to be mixed in as a
  * stackable trait.
  */
trait LoggingCache[F[_], V] extends AbstractCache[F, String, V] {
  val F: Sync[F]

  var (getCalledWithArgs, putCalledWithArgs, removeCalledWithArgs) =
    (ArrayBuffer.empty[String], ArrayBuffer.empty[(String, Any, Option[Duration])], ArrayBuffer.empty[String])

  protected abstract override def doGet(key: String): F[Option[V]] = F.defer {
    getCalledWithArgs.append(key)
    super.doGet(key)
  }

  protected abstract override def doPut(key: String, value: V, ttl: Option[Duration]): F[Unit] = F.defer {
    putCalledWithArgs.append((key, value, ttl))
    super.doPut(key, value, ttl)
  }

  protected abstract override def doRemove(key: String): F[Unit] = F.defer {
    removeCalledWithArgs.append(key)
    super.doRemove(key)
  }

  val reset: F[Unit] = F.delay {
    getCalledWithArgs.clear()
    putCalledWithArgs.clear()
    removeCalledWithArgs.clear()
  }

}

/** A mock cache that keeps track of the arguments it was called with.
  */
class LoggingMockCache[F[_]: Sync, V] extends MockCache[F, V] with LoggingCache[F, V]
