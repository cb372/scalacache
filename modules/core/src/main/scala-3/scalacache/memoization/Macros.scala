package scalacache.memoization

import scala.concurrent.duration.Duration
import scala.quoted.*

import scalacache.{Cache, Flags}

object Macros {
  def memoizeImpl[F[_], V](
    ttl: Expr[Option[Duration]], f: Expr[V], cache: Expr[Cache[F, V]], flags: Expr[Flags]
  )(using Quotes, Type[F], Type[V]): Expr[F[V]] =
    commonMacroImpl(cache, keyName => '{ $ { cache }.cachingForMemoize($ { keyName })($ { ttl })($ { f })($ { flags }) })

  def memoizeFImpl[F[_], V](
    ttl: Expr[Option[Duration]], f: Expr[F[V]], cache: Expr[Cache[F, V]], flags: Expr[Flags]
  )(using Quotes, Type[F], Type[V]): Expr[F[V]] =
    commonMacroImpl(cache, keyName => '{ $ { cache }.cachingForMemoizeF($ { keyName })($ { ttl })($ { f })($ { flags }) })

  private def commonMacroImpl[F[_], V](
      cache: Expr[Cache[F, V]],
      keyNameToCachingCall: Expr[String] => Expr[F[V]]
  )(using Quotes, Type[F], Type[V]): Expr[F[V]] = {
    import quotes.reflect.*
    val sym      = Symbol.spliceOwner.owner
    val defdef   = sym.tree.asInstanceOf[DefDef]
    def getOwningClass(s: Symbol): ClassDef = if (s.isClassDef) s.tree.asInstanceOf[ClassDef] else getOwningClass(s.owner)
    val classdef = getOwningClass(sym)
    val defParams: Seq[Seq[Expr[Any]]] = defdef.paramss.map(_.params.collect {
      case p: ValDef  => Ref(p.symbol).asExpr
//      case _: TypeDef => q.abort("Dying on method type param")
    })
    val classParams: Seq[Seq[Expr[Any]]] = classdef.constructor.paramss.map(_.params.collect {
      case p: ValDef  => Ref(p.symbol).asExpr
//      case _: TypeDef => q.abort("Dying on class type param")
    })
    def traverse[V](coll: Seq[Expr[V]])(using Type[V]): Expr[IndexedSeq[V]] = coll.foldLeft( '{
        IndexedSeq.empty[V]
      }) {
      case (acc, next) => '{ $ { acc } :+ $ { next } }
    }
    val defParamExpr: Expr[IndexedSeq[IndexedSeq[Any]]]   = traverse(defParams map traverse)
    val classParamExpr: Expr[IndexedSeq[IndexedSeq[Any]]] = traverse(classParams map traverse)
    val keyValue: Expr[String] = '{${cache}.config.memoization.toStringConverter.toString(
      ${ Expr(classdef.name) },
      $defParamExpr,
      ${ Expr(defdef.name) },
      $classParamExpr
    )}
    val cachingCallAsThing: Ref => Term = { case i: Ident =>
      keyNameToCachingCall(i.asExpr.asInstanceOf[Expr[String]]).asTerm
    }
    ValDef.let(Symbol.spliceOwner, "key", keyValue.asTerm)(cachingCallAsThing).asExpr.asInstanceOf[Expr[F[V]]]
  }
}
