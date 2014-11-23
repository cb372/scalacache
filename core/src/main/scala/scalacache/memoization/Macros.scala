package scalacache.memoization

import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import scala.concurrent.duration.Duration
import scalacache.{ Flags, ScalaCache }

object Macros {

  def memoizeImpl[A: c.WeakTypeTag](c: blackbox.Context)(f: c.Expr[A])(scalaCache: c.Expr[ScalaCache], flags: c.Expr[Flags]) = {
    import c.universe._

    commonMacroImpl(c)(scalaCache, { keyName =>
      q"""_root_.scalacache.caching($keyName)($f)($scalaCache, $flags)"""
    })
  }

  def memoizeImplWithTTL[A: c.WeakTypeTag](c: blackbox.Context)(ttl: c.Expr[Duration])(f: c.Expr[A])(scalaCache: c.Expr[ScalaCache], flags: c.Expr[Flags]) = {
    import c.universe._

    commonMacroImpl(c)(scalaCache, { keyName =>
      q"""_root_.scalacache.cachingWithTTL($keyName)($ttl)($f)($scalaCache, $flags)"""
    })
  }

  private def commonMacroImpl[A: c.WeakTypeTag](c: blackbox.Context)(scalaCache: c.Expr[ScalaCache], keyNameToCachingCall: c.TermName => c.Tree) = {
    import c.universe._

    val enclosingMethodSymbol = getMethodSymbol(c)
    val classSymbol = getClassSymbol(c)

    /*
     * Gather all the info needed to build the cache key:
     * class name, method name and the method parameters lists
     */
    val classNameTree = getFullClassName(c)(classSymbol)
    val classParamssTree = getConstructorParams(c)(classSymbol)
    val methodNameTree = getMethodName(c)(enclosingMethodSymbol)
    val methodParamssSymbols = c.internal.enclosingOwner.info.paramLists
    val methodParamssTree = paramListsToTree(c)(methodParamssSymbols)

    val keyName = createKeyName(c)
    val scalacacheCall = keyNameToCachingCall(keyName)
    val tree = q"""
          val $keyName = $scalaCache.memoization.toStringConvertor.toString($classNameTree, $classParamssTree, $methodNameTree, $methodParamssTree)
          $scalacacheCall
        """
    //println(showCode(tree))
    //println(showRaw(tree, printIds = true, printTypes = true))
    tree
  }

  /**
   * Get the symbol of the method that encloses the macro,
   * or abort the compilation if we can't find one.
   */
  private def getMethodSymbol(c: blackbox.Context): c.Symbol = {
    import c.universe._

    def getMethodSymbolRecursively(sym: Symbol): Symbol = {
      if (sym == null || sym == NoSymbol || sym.owner == sym)
        c.abort(c.enclosingPosition,
          "This memoize block does not appear to be inside a method. " +
            "Memoize blocks must be placed inside methods, so that a cache key can be generated.")
      else if (sym.isMethod)
        sym
      else
        getMethodSymbolRecursively(sym.owner)
    }

    getMethodSymbolRecursively(c.internal.enclosingOwner)
  }

  /**
   * Convert the given method symbol to a tree representing the method name.
   */
  private def getMethodName(c: blackbox.Context)(methodSymbol: c.Symbol): c.Tree = {
    import c.universe._
    val methodName = methodSymbol.asMethod.name.toString
    // return a Tree
    q"$methodName"
  }

  private def getClassSymbol(c: blackbox.Context): c.Symbol = {
    import c.universe._

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

  /**
   * Convert the given class symbol to a tree representing the fully qualified class name.
   *
   * @param classSymbol should be either a ClassSymbol or a ModuleSymbol
   */
  private def getFullClassName(c: blackbox.Context)(classSymbol: c.Symbol): c.Tree = {
    import c.universe._
    val className = classSymbol.fullName
    // return a Tree
    q"$className"
  }

  private def getConstructorParams(c: blackbox.Context)(classSymbol: c.Symbol): c.Tree = {
    import c.universe._
    if (classSymbol.isClass) {
      val symbolss = classSymbol.asClass.primaryConstructor.asMethod.paramLists
      if (symbolss == List(Nil)) {
        q"_root_.scala.collection.immutable.Nil"
      } else {
        paramListsToTree(c)(symbolss)
      }
    } else {
      q"_root_.scala.collection.immutable.Nil"
    }
  }

  private def paramListsToTree(c: blackbox.Context)(symbolss: List[List[c.Symbol]]): c.Tree = {
    import c.universe._
    val identss: List[List[Ident]] = symbolss.map(ss => ss.map(s => Ident(s.name)))
    listToTree(c)(identss.map(is => listToTree(c)(is)))
  }

  /**
   * Convert a List[Tree] to a Tree by calling scala.collection.immutable.list.apply()
   */
  private def listToTree(c: blackbox.Context)(ts: List[c.Tree]): c.Tree = {
    import c.universe._
    q"_root_.scala.collection.immutable.List(..$ts)"
  }

  private def createKeyName(c: blackbox.Context) = {
    // We must create a fresh name for any vals that we define, to ensure we don't clash with any user-defined terms.
    // See https://github.com/cb372/scalacache/issues/13
    // (Note that c.freshName("key") does not work as expected.
    // It causes quasiquotes to generate crazy code, resulting in a MatchError.)
    c.freshName(c.universe.TermName("key"))
  }

}
