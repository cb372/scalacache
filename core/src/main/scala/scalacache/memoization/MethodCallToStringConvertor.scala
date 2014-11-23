package scalacache.memoization

/**
 * Converts information about a method call to a String for use in a cache key
 */
trait MethodCallToStringConvertor {

  /**
   * Convert the given method call information to a String for use in a cache key
   * @param fullClassName the name of the class whose method was called, including fully-qualified package name
   * @param constructorParamss
   *                the values of the constructor parameters of the class containing the method, where applicable.
   *                This is a `[Seq[Seq[Any]]` because there may be multiple parameter lists
   * @param methodName the name of the called method
   * @param paramss
   *                the values of the parameters that were passed to the method.
   *                This is a `[Seq[Seq[Any]]` because there may be multiple parameter lists
   * @return
   */
  def toString(fullClassName: String, constructorParamss: Seq[Seq[Any]], methodName: String, paramss: Seq[Seq[Any]]): String

}

object MethodCallToStringConvertor {

  private def classNamePart(className: String) =
    if (className.isEmpty) "" else className + "."

  private def classNameAndParamsPart(className: String, constructorParamss: Seq[Seq[Any]]) =
    if (className.isEmpty) "" else className + paramssPart(constructorParamss) + "."

  private def paramssPart(paramss: Seq[Seq[Any]]) =
    paramss.map(_.mkString("(", ", ", ")")).mkString("", "", "")

  /**
   * A cache key generator that builds keys of the form: "package.class.method(arg, ...)(arg, ...)..."
   * e.g. "com.foo.MyClass.doSomething(123, abc)(foo)"
   *
   * Note that this converter ignores the class's constructor params and does NOT include them in the cache key.
   */
  val defaultConvertor: MethodCallToStringConvertor = new MethodCallToStringConvertor {
    def toString(fullClassName: String, constructorParamss: Seq[Seq[Any]], methodName: String, paramss: Seq[Seq[Any]]): String =
      s"${classNamePart(fullClassName)}$methodName${paramssPart(paramss)}"
  }

  /**
   * A cache key generator that builds keys of the form: "package.class(arg, ...)(arg, ...).method(arg, ...)(arg, ...)..."
   * e.g. "com.foo.MyClass.doSomething(123, abc)(foo)"
   *
   * Note that this converter includes the class's constructor params in the cache key, where applicable.
   */
  val includeClassConstructorParams = new MethodCallToStringConvertor {
    def toString(fullClassName: String, constructorParamss: Seq[Seq[Any]], methodName: String, paramss: Seq[Seq[Any]]): String =
      s"${classNameAndParamsPart(fullClassName, constructorParamss)}$methodName${paramssPart(paramss)}"
  }

}

