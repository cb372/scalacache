package scalacache.memoization

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import scala.concurrent.duration.Duration
import scalacache.{ Flags, ScalaCache }

object Macros {

  def memoizeImpl[A: c.WeakTypeTag](c: Context)(f: c.Expr[A])(scalaCache: c.Expr[ScalaCache], flags: c.Expr[Flags]) = {
    import c.universe._

    val enclosingMethodSymbol = getMethodSymbol(c)

    /*
     * Gather all the info needed to build the cache key:
     * class name, method name and the method parameters lists
     */
    val classNameTree = getClassName(c)
    val methodNameTree = getMethodName(c)(enclosingMethodSymbol)
    val paramssSymbols = c.internal.enclosingOwner.info.paramLists
    val paramssIdents: List[List[Ident]] = paramssSymbols.map(ps => ps.map(p => Ident(p.name)))
    val paramssTree = listToTree(c)(paramssIdents.map(ps => listToTree(c)(ps)))

    val keyName = createKeyName(c)
    val tree = q"""
          val $keyName = $scalaCache.memoization.toStringConvertor.toString($classNameTree, scala.collection.immutable.Nil, $methodNameTree, $paramssTree)
          scalacache.caching($keyName)($f)($scalaCache, $flags)
        """
    //println(showCode(tree))
    //println(showRaw(tree, printIds = true, printTypes = true))
    tree
  }

  def memoizeImplWithTTL[A: c.WeakTypeTag](c: Context)(ttl: c.Expr[Duration])(f: c.Expr[A])(scalaCache: c.Expr[ScalaCache], flags: c.Expr[Flags]) = {
    import c.universe._

    val enclosingMethodSymbol = getMethodSymbol(c)

    /*
     * Gather all the info needed to build the cache key:
     * class name, method name and the method parameters lists
     */
    val classNameTree = getClassName(c)
    val methodNameTree = getMethodName(c)(enclosingMethodSymbol)
    val paramssSymbols = c.internal.enclosingOwner.info.paramLists
    val paramssIdents: List[List[Ident]] = paramssSymbols.map(ps => ps.map(p => Ident(p.name)))
    val paramssTree = listToTree(c)(paramssIdents.map(ps => listToTree(c)(ps)))

    val keyName = createKeyName(c)
    val tree = q"""
          val $keyName = $scalaCache.memoization.toStringConvertor.toString($classNameTree, scala.collection.immutable.Nil, $methodNameTree, $paramssTree)
          scalacache.cachingWithTTL($keyName)($ttl)($f)($scalaCache, $flags)
        """
    //println(showCode(tree))
    //println(showRaw(tree, printIds = true, printTypes = true))
    tree
  }

  /**
   * Get the symbol of the method that encloses the macro,
   * or abort the compilation if we can't find one.
   */
  private def getMethodSymbol(c: Context): c.Symbol = {
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

  private def getClassName(c: Context): c.Tree = {
    import c.universe._

    def getClassNameRecursively(sym: Symbol): String = {
      if (sym == null)
        c.abort(c.enclosingPosition, "Encountered a null symbol while searching for enclosing class")
      else if (sym.isClass)
        sym.asClass.fullName
      else if (sym.isModule)
        sym.asModule.fullName
      else
        getClassNameRecursively(sym.owner)
    }

    val className = getClassNameRecursively(c.internal.enclosingOwner)

    // return a Tree
    q"$className"
  }

  /**
   * Convert the given method symbol to a tree representing the method name.
   */
  private def getMethodName(c: Context)(methodSymbol: c.Symbol): c.Tree = {
    import c.universe._

    val methodName = methodSymbol.asMethod.name.toString

    // return a Tree
    q"$methodName"
  }

  /**
   * Convert a List[Tree] to a Tree by calling scala.collection.immutable.list.apply()
   */
  private def listToTree(c: Context)(ts: List[c.Tree]): c.Tree = {
    import c.universe._

    q"_root_.scala.collection.immutable.List(..$ts)"
  }

  private def createKeyName(c: Context) = {
    // We must create a fresh name for any vals that we define, to ensure we don't clash with any user-defined terms.
    // See https://github.com/cb372/scalacache/issues/13
    // (Note that c.freshName("key") does not work as expected.
    // It causes quasiquotes to generate crazy code, resulting in a MatchError.)
    c.freshName(c.universe.TermName("key"))
  }

}
