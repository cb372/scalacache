package scalacache.memoization

/**
 * Converts information about a method call to a String for use in a cache key
 */
trait MethodCallToStringConvertor {

  /**
   * Convert the given method call information to a String for use in a cache key
   * @param fullClassName the name of the class whose method was called, including fully-qualified package name
   * @param methodName the name of the called method
   * @param paramss
   *                the values of the parameters that were passed to the method.
   *                This is a `[Seq[Seq[Any]]` because there may be multiple parameter lists
   * @return
   */
  def toString(fullClassName: String, methodName: String, paramss: Seq[Seq[Any]]): String

}

object MethodCallToStringConvertor {

  /**
   * A cache key generator that builds keys of the form: "package.class.method(arg, ...)(arg, ...)..."
   * e.g. "com.foo.MyClass.doSomething(123, abc)(foo)"
   */
  implicit val defaultConvertor: MethodCallToStringConvertor = new MethodCallToStringConvertor {
    private def classNamePart(className: String) =
      if (className.isEmpty) "" else className + "."

    private def paramssPart(paramss: Seq[Seq[Any]]) =
      paramss.map(_.mkString("(", ", ", ")")).mkString("", "", "")

    def toString(fullClassName: String, methodName: String, paramss: Seq[Seq[Any]]): String =
      s"${classNamePart(fullClassName)}${methodName}${paramssPart(paramss)}"
  }

}

