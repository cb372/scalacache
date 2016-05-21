package scalacache.memoization

/**
 * Converts information about a method call to a String for use in a cache key
 */
trait MethodCallToStringConverter {

  /**
   * Convert the given method call information to a String for use in a cache key
   * @param fullClassName the name of the class whose method was called, including fully-qualified package name
   * @param constructorParamss
   *                the values of the constructor parameters of the method's enclosing class, where applicable.
   *                This is a `[Seq[Seq[Any]]` because there may be multiple parameter lists.
   *                If the method is inside an `object`, a `trait` or a class with no constructor params, this will be `Nil`.
   * @param methodName the name of the called method
   * @param paramss
   *                the values of the parameters that were passed to the method.
   *                This is a `[Seq[Seq[Any]]` because there may be multiple parameter lists
   * @return
   */
  def toString(fullClassName: String, constructorParamss: Seq[Seq[Any]], methodName: String, paramss: Seq[Seq[Any]]): String

}

object MethodCallToStringConverter {

  private def classNamePart(className: String) =
    if (className.isEmpty) "" else className + "."

  private def classNameAndParamsPart(className: String, constructorParamss: Seq[Seq[Any]]) =
    if (className.isEmpty) "" else className + paramssPart(constructorParamss) + "."

  private def paramssPart(paramss: Seq[Seq[Any]]) =
    paramss.map(_.mkString("(", ", ", ")")).mkString("", "", "")

  /**
   * A converter that builds keys of the form: "package.class.method(arg, ...)(arg, ...)..."
   * e.g. "com.foo.MyClass.doSomething(123, abc)(foo)"
   *
   * Note that this converter ignores the class's constructor params and does NOT include them in the cache key.
   */
  val excludeClassConstructorParams: MethodCallToStringConverter = new MethodCallToStringConverter {
    def toString(fullClassName: String, constructorParamss: Seq[Seq[Any]], methodName: String, paramss: Seq[Seq[Any]]): String =
      s"${classNamePart(fullClassName)}$methodName${paramssPart(paramss)}"
  }

  /**
   * A converter that builds keys of the form: "package.class(arg, ...)(arg, ...).method(arg, ...)(arg, ...)..."
   * e.g. "com.foo.MyClass(42, wow).doSomething(123, abc)(foo)"
   *
   * Note that this converter includes the class's constructor params in the cache key, where applicable.
   */
  val includeClassConstructorParams = new MethodCallToStringConverter {
    def toString(fullClassName: String, constructorParamss: Seq[Seq[Any]], methodName: String, paramss: Seq[Seq[Any]]): String =
      s"${classNameAndParamsPart(fullClassName, constructorParamss)}$methodName${paramssPart(paramss)}"
  }

  /**
   * A converter that includes only the method arguments in the cache key.
   * It builds keys of the form: "(arg, ...)(arg, ...)..."
   * e.g. a call to `com.foo.MyClass(42, wow).doSomething(123, abc)(foo)` would be cached as "(123, abc)(foo)".
   *
   * Warning: Do not use this key if you have multiple methods that you want to memoize, because cache keys can collide.
   * e.g. the results of `Foo.bar(123)` and `Baz.wow(123)` would be cached with the same key `123`.
   */
  val onlyMethodParams = new MethodCallToStringConverter {
    def toString(fullClassName: String, constructorParamss: Seq[Seq[Any]], methodName: String, paramss: Seq[Seq[Any]]): String =
      paramssPart(paramss)
  }

}

