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

package scalacache.memoization

import scala.annotation.StaticAnnotation

/** Add this annotation to method or class constructor parameters in order to exclude them from auto-generated cache
  * keys.
  *
  * e.g.
  *
  * {{{
  * def foo(a: Int, @cacheKeyExclude b: String, c: String): Int = memoize { ... }
  * }}}
  *
  * will not include the value of the `b` parameter in its cache keys.
  */
final class cacheKeyExclude extends StaticAnnotation
