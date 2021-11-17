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

import scala.concurrent.duration.Duration
import scala.quoted.*

import scalacache.{Cache, Flags}

object Macros {
  def memoizeImpl[F[_], V](
      ttl: Expr[Option[Duration]],
      f: Expr[V],
      cache: Expr[Cache[F, String, V]],
      config: Expr[MemoizationConfig],
      flags: Expr[Flags]
  )(using Quotes, Type[F], Type[V]): Expr[F[V]] =
    commonMacroImpl(config, keyName => '{ ${ cache }.caching(${ keyName })(${ ttl })(${ f })(${ flags }) })

  def memoizeFImpl[F[_], V](
      ttl: Expr[Option[Duration]],
      f: Expr[F[V]],
      cache: Expr[Cache[F, String, V]],
      config: Expr[MemoizationConfig],
      flags: Expr[Flags]
  )(using Quotes, Type[F], Type[V]): Expr[F[V]] =
    commonMacroImpl(config, keyName => '{ ${ cache }.cachingF(${ keyName })(${ ttl })(${ f })(${ flags }) })

  private def commonMacroImpl[F[_], V](
      config: Expr[MemoizationConfig],
      keyNameToCachingCall: Expr[String] => Expr[F[V]]
  )(using Quotes, Type[F], Type[V]): Expr[F[V]] = {
    import quotes.reflect.*
    val sym: Symbol = Symbol.spliceOwner

    def hasCacheKeyExcludeAnnotation(s: Symbol): Boolean = s.annotations.exists {
      case Apply(Select(New(TypeIdent("cacheKeyExclude")), _), _) => true
      case o                                                      => false
    }

    def getOwningDefSymbol(s: Symbol): Symbol =
      if (s.isDefDef || s == Symbol.noSymbol) s else getOwningDefSymbol(s.owner)

    val defdef: DefDef = getOwningDefSymbol(sym).tree.asInstanceOf[DefDef]

    def getOwningClassSymbol(s: Symbol): Symbol = if (s.isClassDef) s else getOwningClassSymbol(s.owner)

    val classdefSymbol = getOwningClassSymbol(sym)

    val classdef: ClassDef = classdefSymbol.tree.asInstanceOf[ClassDef]

    val defParams: Seq[Seq[Expr[Any]]] = defdef.termParamss.map(
      _.params
        .filterNot(p => hasCacheKeyExcludeAnnotation(p.symbol))
        .map(p => Ref(p.symbol).asExpr)
    )

    val classParams: Seq[Seq[Expr[Any]]] = classdef.constructor.termParamss.map(
      _.params
        .filterNot(p => hasCacheKeyExcludeAnnotation(p.symbol))
        .map { p => Ref.term(TermRef(This(classdef.symbol).tpe, p.name)).asExpr }
    ) match {
      case seqseq if seqseq.forall(_.isEmpty) => Nil
      case seqseq                             => seqseq
    }

    def traverse[V](coll: Seq[Expr[V]])(using Type[V]): Expr[IndexedSeq[V]] = {
      val v = Varargs(coll)
      '{ IndexedSeq($v: _*) }
    }

    val defParamExpr: Expr[IndexedSeq[IndexedSeq[Any]]] = traverse(defParams map traverse)

    val classParamExpr: Expr[IndexedSeq[IndexedSeq[Any]]] = traverse(classParams map traverse)

    val fullName = Expr(classdefSymbol.fullName)
    val name     = Expr(defdef.name)
    val keyValue: Expr[String] = '{
      ${ config }.toStringConverter.toString(
        $fullName,
        $classParamExpr,
        $name,
        $defParamExpr
      )
    }

    keyNameToCachingCall(keyValue)
  }
}
