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

package scalacache.serialization.binary

import scala.reflect.ClassTag

trait BinaryAnyRefCodecs_1 {

  /*
  String and Array[Byte] extend java.io.Serializable,
  so this implicit needs to be lower priority than those in BinaryPrimitiveCodecs
   */
  implicit def anyRefBinaryCodec[S <: java.io.Serializable](implicit ev: ClassTag[S]): BinaryCodec[S] =
    new JavaSerializationAnyRefCodec[S](ev)

}

trait BinaryAnyRefCodecs_0 extends BinaryAnyRefCodecs_1
