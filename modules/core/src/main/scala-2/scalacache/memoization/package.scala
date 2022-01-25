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

import scala.concurrent.duration._

/** Utilities for memoizing the results of method calls in a cache. The cache key is generated from the method arguments
  * using a macro, so that you don't have to bother passing them manually.
  */
package object memoization {

  /** Perform the given operation and memoize its result to a cache before returning it. If the result is already in the
    * cache, return it without performing the operation.
    *
    * If a TTL is given, the result is stored in the cache with that TTL. It will be evicted when the TTL is up.
    *
    * Note that if the result is currently in the cache, changing the TTL has no effect. TTL is only set once, when the
    * result is added to the cache.
    *
    * @param ttl
    *   Time-To-Live
    * @param f
    *   A function that computes some result. This result is the value that will be cached.
    * @param cache
    *   The cache
    * @param flags
    *   Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam F
    *   The type of container in which the result will be wrapped. This is decided by the mode.
    * @tparam V
    *   The type of the value to be cached
    * @return
    *   A result, either retrieved from the cache or calculated by executing the function `f`
    */
  def memoize[F[_], V](ttl: Option[Duration])(
      f: => V
  )(implicit cache: Cache[F, String, V], config: MemoizationConfig, flags: Flags): F[V] =
    macro Macros.memoizeImpl[F, V]

  /** Perform the given operation and memoize its result to a cache before returning it. If the result is already in the
    * cache, return it without performing the operation.
    *
    * If a TTL is given, the result is stored in the cache with that TTL. It will be evicted when the TTL is up.
    *
    * Note that if the result is currently in the cache, changing the TTL has no effect. TTL is only set once, when the
    * result is added to the cache.
    *
    * @param ttl
    *   Time-To-Live
    * @param f
    *   A function that computes some result wrapped in an `F`. This result is the value that will be cached.
    * @param cache
    *   The cache
    * @param flags
    *   Flags used to conditionally alter the behaviour of ScalaCache
    * @tparam F
    *   The type of container in which the result will be wrapped. This is decided by the mode.
    * @tparam V
    *   The type of the value to be cached
    * @return
    *   A result, either retrieved from the cache or calculated by executing the function `f`
    */
  def memoizeF[F[_], V](
      ttl: Option[Duration]
  )(f: F[V])(implicit cache: Cache[F, String, V], config: MemoizationConfig, flags: Flags): F[V] =
    macro Macros.memoizeFImpl[F, V]
}
