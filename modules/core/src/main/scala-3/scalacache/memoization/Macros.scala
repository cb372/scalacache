package scalacache.memoization

import scala.concurrent.duration.Duration
import scala.quoted.*

import scalacache.{Cache, Flags}

object Macros {
  def memoizeImpl[F[_], V](
      ttl: Expr[Option[Duration]],
      f: Expr[V],
      cache: Expr[Cache[F, V]],
      flags: Expr[Flags]
  )(using Quotes, Type[F], Type[V]): Expr[F[V]] =
    commonMacroImpl(cache, keyName => '{ ${ cache }.cachingForMemoize(${ keyName })(${ ttl })(${ f })(${ flags }) })

  def memoizeFImpl[F[_], V](
      ttl: Expr[Option[Duration]],
      f: Expr[F[V]],
      cache: Expr[Cache[F, V]],
      flags: Expr[Flags]
  )(using Quotes, Type[F], Type[V]): Expr[F[V]] =
    commonMacroImpl(cache, keyName => '{ ${ cache }.cachingForMemoizeF(${ keyName })(${ ttl })(${ f })(${ flags }) })

  private def commonMacroImpl[F[_], V](
      cache: Expr[Cache[F, V]],
      keyNameToCachingCall: Expr[String] => Expr[F[V]]
  )(using Quotes, Type[F], Type[V]): Expr[F[V]] = {
    import quotes.reflect.*
    val sym: Symbol = Symbol.spliceOwner

    def hasCacheKeyExcludeAnnotation(s: Symbol): Boolean = s.annotations.exists {
//      case Apply(Select(New(Ident("cacheKeyExclude")), _), _) => true // Don't know why this doesn't match...
      case Apply(Select(New(o), _), _) => o.toString == "Ident(cacheKeyExclude)"
      case o                           => false
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

    def traverse[V](coll: Seq[Expr[V]])(using Type[V]): Expr[IndexedSeq[V]] =
      coll.foldLeft('{ IndexedSeq.empty[V] }) { case (acc, next) => '{ ${ acc } :+ ${ next } } }

    val defParamExpr: Expr[IndexedSeq[IndexedSeq[Any]]] = traverse(defParams map traverse)

    val classParamExpr: Expr[IndexedSeq[IndexedSeq[Any]]] = traverse(classParams map traverse)

    val keyValue: Expr[String] = '{
      ${ cache }.config.memoization.toStringConverter.toString(
        ${ Expr(classdefSymbol.fullName) },
        $classParamExpr,
        ${ Expr(defdef.name) },
        $defParamExpr
      )
    }

    keyNameToCachingCall(keyValue)
  }
}
