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

import scalacache.Cache
import scalacache.Flags

import scala.concurrent.duration.Duration
import scala.reflect.macros.blackbox

class Macros(val c: blackbox.Context) {
  import c.universe._

  def memoizeImpl[F[_], V](
      ttl: c.Expr[Option[Duration]]
  )(f: c.Tree)(cache: c.Expr[Cache[F, String, V]], config: c.Expr[MemoizationConfig], flags: c.Expr[Flags]): c.Tree = {
    commonMacroImpl(
      config,
      { keyName =>
        q"""$cache.caching($keyName)($ttl)($f)($flags)"""
      }
    )
  }

  def memoizeFImpl[F[_], V](
      ttl: c.Expr[Option[Duration]]
  )(f: c.Tree)(cache: c.Expr[Cache[F, String, V]], config: c.Expr[MemoizationConfig], flags: c.Expr[Flags]): c.Tree = {
    commonMacroImpl(
      config,
      { keyName =>
        q"""$cache.cachingF($keyName)($ttl)($f)($flags)"""
      }
    )
  }

  private def commonMacroImpl[F[_], V](
      config: c.Expr[MemoizationConfig],
      keyNameToCachingCall: (c.TermName) => c.Tree
  ): Tree = {

    val enclosingMethodSymbol = getMethodSymbol()
    val classSymbol           = getClassSymbol()

    /*
     * Gather all the info needed to build the cache key:
     * class name, method name and the method parameters lists
     */
    val classNameTree        = getFullClassName(classSymbol)
    val classParamssTree     = getConstructorParams(classSymbol)
    val methodNameTree       = getMethodName(enclosingMethodSymbol)
    val methodParamssSymbols = c.internal.enclosingOwner.info.paramLists
    val methodParamssTree    = paramListsToTree(methodParamssSymbols)

    val keyName     = createKeyName()
    val cachingCall = keyNameToCachingCall(keyName)
    val tree = q"""
          val $keyName = $config.toStringConverter.toString($classNameTree, $classParamssTree, $methodNameTree, $methodParamssTree)
          $cachingCall
        """
    // println(showCode(tree))
    // println(showRaw(tree, printIds = true, printTypes = true))
    tree
  }

  /** Get the symbol of the method that encloses the macro, or abort the compilation if we can't find one.
    */
  private def getMethodSymbol(): c.Symbol = {

    def getMethodSymbolRecursively(sym: Symbol): Symbol = {
      if (sym == null || sym == NoSymbol || sym.owner == sym)
        c.abort(
          c.enclosingPosition,
          "This memoize block does not appear to be inside a method. " +
            "Memoize blocks must be placed inside methods, so that a cache key can be generated."
        )
      else if (sym.isMethod)
        sym
      else
        getMethodSymbolRecursively(sym.owner)
    }

    getMethodSymbolRecursively(c.internal.enclosingOwner)
  }

  /** Convert the given method symbol to a tree representing the method name.
    */
  private def getMethodName(methodSymbol: c.Symbol): c.Tree = {
    val methodName = methodSymbol.asMethod.name.toString
    // return a Tree
    q"$methodName"
  }

  private def getClassSymbol(): c.Symbol = {
    def getClassSymbolRecursively(sym: Symbol): Symbol = {
      if (sym == null)
        c.abort(c.enclosingPosition, "Encountered a null symbol while searching for enclosing class")
      else if (sym.isClass || sym.isModule)
        sym
      else
        getClassSymbolRecursively(sym.owner)
    }

    getClassSymbolRecursively(c.internal.enclosingOwner)
  }

  /** Convert the given class symbol to a tree representing the fully qualified class name.
    *
    * @param classSymbol
    *   should be either a ClassSymbol or a ModuleSymbol
    */
  private def getFullClassName(classSymbol: c.Symbol): c.Tree = {
    val className = classSymbol.fullName
    // return a Tree
    q"$className"
  }

  private def getConstructorParams(classSymbol: c.Symbol): c.Tree = {
    if (classSymbol.isClass) {
      val symbolss = classSymbol.asClass.primaryConstructor.asMethod.paramLists
      if (symbolss == List(Nil)) {
        q"_root_.scala.collection.immutable.Vector.empty"
      } else {
        paramListsToTree(symbolss)
      }
    } else {
      q"_root_.scala.collection.immutable.Vector.empty"
    }
  }

  private def paramListsToTree(symbolss: List[List[c.Symbol]]): c.Tree = {
    val cacheKeyExcludeType = c.typeOf[cacheKeyExclude]
    def shouldExclude(s: c.Symbol) = {
      s.annotations.exists(a => a.tree.tpe == cacheKeyExcludeType)
    }
    val identss: List[List[Ident]] = symbolss.map(ss =>
      ss.collect {
        case s if !shouldExclude(s) => Ident(s.name)
      }
    )
    listToTree(identss.map(is => listToTree(is)))
  }

  /** Convert a List[Tree] to a Tree representing `Vector`
    */
  private def listToTree(ts: List[c.Tree]): c.Tree = {
    q"_root_.scala.collection.immutable.Vector(..$ts)"
  }

  private def createKeyName(): TermName = {
    // We must create a fresh name for any vals that we define, to ensure we don't clash with any user-defined terms.
    // See https://github.com/cb372/scalacache/issues/13
    // (Note that c.freshName("key") does not work as expected.
    // It causes quasiquotes to generate crazy code, resulting in a MatchError.)
    c.freshName(c.universe.TermName("key"))
  }

}
