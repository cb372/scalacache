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

package scalacache.serialization.gzip

import java.io.Serializable

import scala.reflect.ClassTag
import scalacache.serialization.binary.JavaSerializationAnyRefCodec

object GZippingJavaSerializationAnyRefCodec {

  /** Compressing Java generic codec with a threshold of 16K
    */
  implicit def default[S <: Serializable](implicit ev: ClassTag[S]): GZippingJavaSerializationAnyRefCodec[S] =
    new GZippingJavaSerializationAnyRefCodec(CompressingCodec.DefaultSizeThreshold)(ev)

}

class GZippingJavaSerializationAnyRefCodec[S <: Serializable](override val sizeThreshold: Int)(implicit
    classTag: ClassTag[S]
) extends JavaSerializationAnyRefCodec[S](classTag)
    with GZippingBinaryCodec[S]
