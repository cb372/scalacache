package scalacache.memoization

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import scala.concurrent.duration.Duration
import scalacache.ScalaCache
import scala.concurrent.ExecutionContext

object Macros {

  def memoizeImpl[A: c.WeakTypeTag](c: Context)(f: c.Expr[A])(scalaCache: c.Expr[ScalaCache]) = {
    memoizeImplWithTTL[A](c)(c.Expr[Duration](c.parse("scala.concurrent.duration.Duration.Zero")))(f)(scalaCache)
  }

  def memoizeImplWithTTL[A: c.WeakTypeTag](c: Context)(ttl: c.Expr[Duration])(f: c.Expr[A])(scalaCache: c.Expr[ScalaCache]) = {
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

    val tree = q"""
          val key = $scalaCache.memoization.toStringConvertor.toString($classNameTree, $methodNameTree, $paramssTree)
          val ttlOpt = if ($ttl == scala.concurrent.duration.Duration.Zero) None else Some($ttl)
          scalacache.withCaching(key, ttlOpt)($f)($scalaCache)
        """
    //println(showCode(tree))
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

}
