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

import org.scalatest.flatspec.AnyFlatSpec
import scalacache.memoization.MethodCallToStringConverter.onlyMethodParams

class CacheKeyIncludingOnlyMethodParamsSpec extends AnyFlatSpec with CacheKeySpecCommon {

  behavior of "cache key generation for method memoization (only including method params in cache key)"

  override implicit lazy val config: MemoizationConfig = MemoizationConfig(toStringConverter = onlyMethodParams)

  it should "include values of all arguments for all argument lists" in {
    checkCacheKey("(1, 2)(3, 4)") {
      multipleArgLists(1, "2")("3", 4)
    }
  }

}
