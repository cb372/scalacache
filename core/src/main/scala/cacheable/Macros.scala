package cacheable

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import scala.concurrent.duration.Duration

object Macros {

  def cacheableImpl[A : c.WeakTypeTag](c: Context)(f: c.Expr[A])(cacheConfig: c.Expr[CacheConfig]) = {
    cacheableImplWithTTL[A](c)(c.Expr[Duration](c.parse("scala.concurrent.duration.Duration.Zero")))(f)(cacheConfig)
  }

  def cacheableImplWithTTL[A : c.WeakTypeTag](c: Context)(ttl: c.Expr[Duration])(f: c.Expr[A])(cacheConfig: c.Expr[CacheConfig]) = {
    import c.universe._

    /**
     * Convert a List[Tree] to a Tree by calling scala.collection.immutable.list.apply()
     */
    def listToTree(c: Context)(ts: List[Tree]): Tree =
      q"_root_.scala.collection.immutable.List(..$ts)"

    /*
     Unfortunately this is a huge, hairy and unfixable deprecation warning :(

     c.enclosingMethod is due to be removed in Scala 2.12, meaning that there will be no
     way to access the enclosing tree of the macro's call site.
     (c.encosingOwner returns only a Symbol, not a Tree.)

     Apparently this is an intentional API change, as the macro API designers do not want people
     to write macros that care too much about their surroundings.

     Is Cacheable doomed in 2.12 ??!
     */
    c.enclosingMethod match {
      case DefDef(mods, methodName, tparams, vparamss, tpt, rhs) => {

        /*
         * Gather all the info needed to build the cache key:
         * class name, method name and the method parameters lists
         */
        val classNameTree = getClassName(c)
        val methodNameTree = getMethodName(c)
        val paramIdents: List[List[Ident]] = vparamss.map(ps => ps.map(p => Ident(p.name)))
        val paramssTree: Tree = listToTree(c)(paramIdents.map(ps => listToTree(c)(ps)))

        val tree = q"""
          val key = $cacheConfig.keyGenerator.toCacheKey($classNameTree, $methodNameTree, $paramssTree)
          val cachedValue = $cacheConfig.cache.get(key)
          cachedValue.getOrElse {
            // cache miss
            val calculatedValue = $f
            val ttlOpt = if ($ttl == scala.concurrent.duration.Duration.Zero) None else Some($ttl)
            $cacheConfig.cache.put(key, calculatedValue, ttlOpt)
            calculatedValue
          }
        """
        //println(showCode(tree))
        tree
      }

      case _ => {
        // not inside a method
        c.abort(c.enclosingPosition, "This macro must be called from within a method, so that it can generate a cache key. TODO: more useful error message")
      }
    }

  }

  private def getClassName(c: Context) = {
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

  private def getMethodName(c: Context) = {
    import c.universe._

    def getMethodNameRecursively(sym: Symbol): String = {
      if (sym == null)
        c.abort(c.enclosingPosition, "Encountered a null symbol while searching for enclosing method")
      if (sym.isMethod)
        sym.asMethod.name.toString
      else
        getMethodNameRecursively(sym.owner)
    }

    val methodName = getMethodNameRecursively(c.internal.enclosingOwner)

    // return a Tree
    q"$methodName"
  }



}
