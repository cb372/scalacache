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
    def listToTree(c: Context)(ts: List[Tree]): Tree = {
      // TODO could write like this? q"_root_.scala.collection.immutable.List(..$ts)"
      Apply(Select(Select(Select(Select(Ident(TermName("scala")), TermName("collection")), TermName("immutable")), TermName("List")), TermName("apply")), ts)
    }

    c.prefix match {
      case DefDef(mods, methodName, tparams, vparamss, tpt, rhs) => {

        /*
         * Gather all the info needed to build the cache key:
         * class name, method name and the method parameters lists
         */
        //val classNameExpr: Expr[String] = getClassName(c)
        //val methodNameExpr: Expr[String] = c.literal(methodName.toString)
        val classNameTree = getClassName(c)
        val methodNameTree = q"$methodName"
        val paramIdents: List[List[Ident]] = vparamss.map(ps => ps.map(p => Ident(p.name)))
        val paramssTree: Tree = listToTree(c)(paramIdents.map(ps => listToTree(c)(ps)))
        //val paramssExpr: Expr[List[List[Any]]] = c.Expr[List[List[Any]]](paramssTree)

        q"""
          val key = cacheConfig.splice.keyGenerator.toCacheKey($classNameTree, $methodNameTree, $paramssTree)
          val cachedValue = cacheConfig.splice.cache.get[A](key)
          cachedValue.fold[A] {
            // cache miss
            val calculatedValue = f.splice
            val ttlOpt = if (ttl.splice == Duration.Zero) None else Some(ttl.splice)
            cacheConfig.splice.cache.put(key, calculatedValue, ttlOpt)
            calculatedValue
          } { v =>
            // cache hit
            v
          }
        """
//        reify {
//          val key = cacheConfig.splice.keyGenerator.toCacheKey(classNameExpr.splice, methodNameExpr.splice, paramssExpr.splice)
//          val cachedValue = cacheConfig.splice.cache.get[A](key)
//          cachedValue.fold[A] {
//            // cache miss
//            val calculatedValue = f.splice
//            val ttlOpt = if (ttl.splice == Duration.Zero) None else Some(ttl.splice)
//            cacheConfig.splice.cache.put(key, calculatedValue, ttlOpt)
//            calculatedValue
//          } { v =>
//            // cache hit
//            v
//          }
//        }
      
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
      if (sym.isClass)
        sym.asClass.fullName
      else if (sym.isModule)
        sym.asModule.fullName
      else
        // TODO null check
        getClassNameRecursively(sym.owner)
    }

    val className = getClassNameRecursively(c.internal.enclosingOwner)

    // return a Tree
    q"$className"
  }

  private def getMethodName(c: Context) = {
    import c.universe._

    def getMethodNameRecursively(sym: Symbol): String = {
      if (sym.isMethod)
        sym.asMethod.fullName
      else if (sym.isModule)
        sym.asModule.fullName
      else
      // TODO null check
        getMethodNameRecursively(sym.owner)
    }

    val methodName = getMethodNameRecursively(c.internal.enclosingOwner)

    // return a Tree
    q"$methodName"
  }



}
