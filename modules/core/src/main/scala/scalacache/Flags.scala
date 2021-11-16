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

/** Configuration flags for conditionally altering the behaviour of ScalaCache.
  *
  * @param readsEnabled
  *   if false, cache GETs will be skipped (and will return `None`)
  * @param writesEnabled
  *   if false, cache PUTs will be skipped
  */
case class Flags(readsEnabled: Boolean = true, writesEnabled: Boolean = true)

object Flags {

  /** The default flag values. These can be overriden at the call site, e.g.
    *
    * {{{
    *   def foo() {
    *     implicit val myCustomFlags = Flags(...)
    *     val cachedValue = scalacache.get("wow")
    *     ...
    *   }
    * }}}
    */
  implicit val defaultFlags: Flags = Flags()

}
