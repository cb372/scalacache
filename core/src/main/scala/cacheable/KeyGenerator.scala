package cacheable

trait KeyGenerator {

  def toCacheKey(fullClassName: String, methodName: String, paramss: Seq[Seq[Any]]): String
  
}

object KeyGenerator {

  /**
   * A cache key generator that builds keys of the form: "package.class.method(arg, ...)(arg, ...)..."
   * e.g. "com.foo.MyClass.doSomething(123, abc)(foo)"
   */
  implicit val defaultGenerator: KeyGenerator = new KeyGenerator {
    private def classNamePart(className: String) =
      if (className.isEmpty) "" else className + "."

    private def paramssPart(paramss: Seq[Seq[Any]]) =
      paramss.map(_.mkString("(", ", ", ")")).mkString("", "", "")

    def toCacheKey(fullClassName: String, methodName: String, paramss: Seq[Seq[Any]]): String =
      s"${classNamePart(fullClassName)}${methodName}${paramssPart(paramss)}"
  }

}

