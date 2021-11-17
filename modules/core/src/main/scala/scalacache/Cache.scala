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

import scala.concurrent.duration.Duration

/** Abstract algebra describing the operations a cache can perform
  *
  * @tparam F
  *   The effect monad in which all cache operations will be performed.
  * @tparam K
  *   The type of keys stored in the cache.
  * @tparam V
  *   The type of values stored in the cache.
  */
trait Cache[F[_], K, V] {

  /** Get a value from the cache
    *
    * @param key
    *   The cache key
    * @param flags
    *   Flags used to conditionally alter the behaviour of ScalaCache
    * @return
    *   The appropriate value, if it was found in the cache
    */
  def get(key: K)(implicit flags: Flags): F[Option[V]]

  /** Insert a value into the cache, optionally setting a TTL (time-to-live)
    *
    * @param key
    *   The cache key
    * @param value
    *   The value to insert
    * @param ttl
    *   The time-to-live. The cache entry will expire after this time has elapsed.
    * @param flags
    *   Flags used to conditionally alter the behaviour of ScalaCache
    */
  def put(key: K)(value: V, ttl: Option[Duration] = None)(implicit flags: Flags): F[Unit]

  /** Remove the given key and its associated value from the cache, if it exists. If the key is not in the cache, do
    * nothing.
    *
    * @param key
    *   The cache key
    */
  def remove(key: K): F[Unit]

  /** Delete the entire contents of the cache. Use wisely!
    */
  def removeAll: F[Unit]

  /** Get a value from the cache if it exists. Otherwise compute it, insert it into the cache, and return it.
    *
    * @param key
    *   The cache key
    * @param ttl
    *   The time-to-live to use when inserting into the cache. The cache entry will expire after this time has elapsed.
    * @param f
    *   A block that computes the value
    * @param flags
    *   Flags used to conditionally alter the behaviour of ScalaCache
    * @return
    *   The value, either retrieved from the cache or computed
    */
  def caching(key: K)(ttl: Option[Duration])(f: => V)(implicit flags: Flags): F[V]

  /** Get a value from the cache if it exists. Otherwise compute it, insert it into the cache, and return it.
    *
    * @param key
    *   The cache key
    * @param ttl
    *   The time-to-live to use when inserting into the cache. The cache entry will expire after this time has elapsed.
    * @param f
    *   A block that computes the value wrapped in a container
    * @param flags
    *   Flags used to conditionally alter the behaviour of ScalaCache
    * @return
    *   The value, either retrieved from the cache or computed
    */
  def cachingF(key: K)(ttl: Option[Duration])(f: F[V])(implicit flags: Flags): F[V]

  /** You should call this when you have finished using this Cache. (e.g. when your application shuts down)
    *
    * It will take care of gracefully shutting down the underlying cache client.
    *
    * Note that you should not try to use this Cache instance after you have called this method.
    */
  // TODO: Replace with Resource-based API?
  def close: F[Unit]

}
