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

package scalacache.memcached

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

import scala.concurrent.{ExecutionContext, Future}
import scalacache._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ReplaceAndTruncateSanitizerSpec extends AnyFlatSpec with Matchers {
  behavior of "ReplaceAndTruncateSanitizer"

  val sanitizer = new ReplaceAndTruncateSanitizer(maxKeyLength = 10)

  it should "truncate keys from the left if they are longer than maxKeyLength" in {
    val longKey = "0123456789A"
    sanitizer.toValidMemcachedKey(longKey) should be("123456789A")
  }

  it should "replace invalid chars with underscores" in {
    val invalidKey = "abc \t\r\nダメ"
    sanitizer.toValidMemcachedKey(invalidKey) should be("abc______")
  }

  it should "allow symbols in key" in {
    val validKey = "~`!@$"
    sanitizer.toValidMemcachedKey(validKey) should be(validKey)
  }

}

class HashingMemcachedKeySanitizerSpec extends AnyFlatSpec with Matchers with ScalaFutures with IntegrationPatience {
  behavior of "HashingMemcachedKeySanitizer"

  val longString = "lolol&%'(%$)$ほげほげ野郎123**+" * 500

  def hexToBytes(s: String): Array[Byte] =
    s.sliding(2, 2).map(Integer.parseInt(_, 16).toByte).toArray

  it should "return a hexadecimal hashed representation of the argument string" in {
    val hashedValues = for {
      algo <- Seq(MD5, SHA1, SHA256, SHA512)
      hashingSanitizer = HashingMemcachedKeySanitizer(algo)
      hashed           = hashingSanitizer.toValidMemcachedKey(longString)
    } yield hashed
    hashedValues.foreach(hexToBytes) // should work
    hashedValues.forall(_.length < 250) should be(true)
  }

  it should "differentiate between strings made up of non-ASCII characters" in {
    val s1 = "毛泽东"
    val s2 = "김정일"
    val hashedPairs = for {
      algo <- Seq(MD5, SHA1, SHA256, SHA512)
      hashingSanitizer = HashingMemcachedKeySanitizer(algo)
      h1               = hashingSanitizer.toValidMemcachedKey(s1)
      h2               = hashingSanitizer.toValidMemcachedKey(s2)
    } yield (h1, h2)
    hashedPairs.forall(pair => pair._1 != pair._2) should be(true)
  }

  it should "return consistent sanitised keys for the same input even when used concurrently across multiple threads" in {
    implicit val ec = ExecutionContext.Implicits.global
    val seqFHashes = for {
      algo <- Seq(MD5, SHA1, SHA256, SHA512)
      hashingSanitizer = HashingMemcachedKeySanitizer(algo)
    } yield {
      Future.sequence((1 to 300).map(_ => Future { hashingSanitizer.toValidMemcachedKey(longString) }))
    }
    val fSeqHashes = Future.sequence(seqFHashes)
    whenReady(fSeqHashes) { hashess =>
      hashess.foreach(hashes => hashes.distinct.size should be(1))
    }
  }

}
