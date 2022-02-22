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

package scalacache.serialization.bson

import io.circe.Json
import io.circe.JsonNumber
import io.circe.JsonObject
import org.bson.BsonArray
import org.bson.BsonBoolean
import org.bson.BsonDecimal128
import org.bson.BsonDocument
import org.bson.BsonDouble
import org.bson.BsonInt32
import org.bson.BsonInt64
import org.bson.BsonNull
import org.bson.BsonString
import org.bson.BsonValue
import scalacache.serialization.Codec
import scalacache.serialization.FailedToDecode

import scala.jdk.CollectionConverters._

package object circe extends BsonCirceCodec

trait BsonCirceCodec {
  implicit def scalaCacheBsonCodecFromCirceCodec[A](implicit
      encoder: io.circe.Encoder[A],
      decoder: io.circe.Decoder[A]
  ): BsonCodec[A] =
    new BsonCodec[A] {

      override def encode(value: A): BsonValue = {
        val json = encoder(value)
        jsonToBson(json)
      }

      override def decode(bytes: BsonValue): Codec.DecodingResult[A] = {
        val json = bsonToJson(bytes)

        decoder
          .decodeJson(json)
          .left
          .map(FailedToDecode.apply)
      }

    }

  private def bsonToJson(bson: BsonValue): Json = bson match {
    case _: BsonNull        => Json.Null
    case b: BsonBoolean     => Json.fromBoolean(b.getValue)
    case i: BsonInt32       => Json.fromInt(i.getValue)
    case l: BsonInt64       => Json.fromLong(l.getValue)
    case d: BsonDouble      => Json.fromDoubleOrNull(d.getValue)
    case bd: BsonDecimal128 => Json.fromBigDecimal(bd.getValue.bigDecimalValue)
    case s: BsonString      => Json.fromString(s.getValue)
    case a: BsonArray       => Json.fromValues(a.getValues.asScala.map(bsonToJson))
    case d: BsonDocument =>
      Json.fromJsonObject(
        JsonObject.fromIterable(
          d.entrySet.asScala.map { entry =>
            entry.getKey -> bsonToJson(entry.getValue)
          }
        )
      )
  }

  private def jsonToBson(json: Json): BsonValue = {
    val toBsonFolder = new Json.Folder[BsonValue] {
      def onNull: BsonValue =
        new BsonNull()

      def onBoolean(value: Boolean): BsonValue =
        new BsonBoolean(value)

      def onNumber(value: JsonNumber): BsonValue = {
        value.toBigDecimal
          .map { bd =>
            if (bd.isValidInt) new BsonInt32(bd.intValue)
            else if (bd.isValidLong) new BsonInt64(bd.longValue)
            else if (bd.isDecimalDouble) new BsonDouble(bd.doubleValue)
            else new BsonString(bd.toString)
          }
          .getOrElse {
            new BsonString(value.toString)
          }
      }

      def onString(value: String): BsonValue =
        new BsonString(value)

      def onArray(value: Vector[Json]): BsonValue = {
        val bsonArray = new BsonArray()
        value.foreach { v =>
          bsonArray.add(v.foldWith(this))
        }
        bsonArray
      }

      def onObject(value: JsonObject): BsonValue = {
        val bsonDocument = new BsonDocument()
        value.toIterable.foreach { case (k, v) =>
          bsonDocument.append(k, v.foldWith(this))
        }
        bsonDocument
      }

    }

    json.foldWith(toBsonFolder)
  }
}
