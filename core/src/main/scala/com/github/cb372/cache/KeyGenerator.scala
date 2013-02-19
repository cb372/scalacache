package com.github.cb372.cache

trait KeyGenerator {

  def toCacheKey(className: String, methodName: String, paramss: Seq[Seq[Any]]): String
  
}

object KeyGenerator {

  implicit val defaultGenerator: KeyGenerator = new KeyGenerator {
    private def classNamePart(className: String) =
      if (className.isEmpty) "" else className + "."

    private def paramssPart(paramss: Seq[Seq[Any]]) =
      paramss.map(_.mkString("(", ", ", ")")).mkString("", "", "")

    def toCacheKey(className: String, methodName: String, paramss: Seq[Seq[Any]]): String =
      s"${classNamePart(className)}${methodName}${paramssPart(paramss)}"
  }

}

