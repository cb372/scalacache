/*
 * Copyright 2021 scalacache
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package scalacache.memoization

/** Converts information about a method call to a String for use in a cache key
  */
trait MethodCallToStringConverter {

  /** Convert the given method call information to a String for use in a cache key
    *
    * @param fullClassName
    *   the name of the class whose method was called, including fully-qualified package name
    * @param constructorParamss
    *   the values of the constructor parameters of the method's enclosing class, where applicable. This is a
    *   `[IndexedSeq[IndexedSeq[Any]]` because there may be multiple parameter lists. If the method is inside an
    *   `object`, a `trait` or a class with no constructor params, this will be empty.
    * @param methodName
    *   the name of the called method
    * @param paramss
    *   the values of the parameters that were passed to the method. This is a `[IndexedSeq[IndexedSeq[Any]]` because
    *   there may be multiple parameter lists
    * @return
    */
  def toString(
      fullClassName: String,
      constructorParamss: IndexedSeq[IndexedSeq[Any]],
      methodName: String,
      paramss: IndexedSeq[IndexedSeq[Any]]
  ): String

}

object MethodCallToStringConverter {
  import java.lang.{StringBuilder => JStringBuilder}

  private def appendClassNamePart(sb: JStringBuilder)(className: String): Unit = {
    if (className.nonEmpty) {
      sb.append(className.stripSuffix("$"))
      val _ = sb.append('.')
    }
  }

  private def appendClassNameAndParamsPart(
      sb: JStringBuilder
  )(className: String, constructorParamss: IndexedSeq[IndexedSeq[Any]]): Unit = {
    if (className.nonEmpty) {
      sb.append(className.stripSuffix("$"))
      appendParamssPart(sb)(constructorParamss)
      val _ = sb.append('.')
    }
  }

  private def appendParamssPart(sb: JStringBuilder)(paramss: IndexedSeq[IndexedSeq[Any]]): Unit = {
    var i = 0
    while (i < paramss.size) {
      val params = paramss(i)
      appendParamsPart(sb)(params)
      i += 1
    }
  }

  private def appendParamsPart(sb: JStringBuilder)(params: IndexedSeq[Any]): Unit = {
    sb.append('(')
    var i = 0
    // Add all params except the last one, with the separator after each one
    while (i < params.size - 1) {
      sb.append(params(i))
      sb.append(", ")
      i += 1
    }
    // Add the final param
    if (i < params.size) {
      sb.append(params(i))
    }
    val _ = sb.append(')')
  }

  /** A converter that builds keys of the form: "package.class.method(arg, ...)(arg, ...)..." e.g.
    * "com.foo.MyClass.doSomething(123, abc)(foo)"
    *
    * Note that this converter ignores the class's constructor params and does NOT include them in the cache key.
    */
  val excludeClassConstructorParams: MethodCallToStringConverter =
    new MethodCallToStringConverter {
      def toString(
          fullClassName: String,
          constructorParamss: IndexedSeq[IndexedSeq[Any]],
          methodName: String,
          paramss: IndexedSeq[IndexedSeq[Any]]
      ): String = {
        val sb = new JStringBuilder(128)
        appendClassNamePart(sb)(fullClassName)
        sb.append(methodName)
        appendParamssPart(sb)(paramss)
        sb.toString
      }
    }

  /** A converter that builds keys of the form: "package.class(arg, ...)(arg, ...).method(arg, ...)(arg, ...)..." e.g.
    * "com.foo.MyClass(42, wow).doSomething(123, abc)(foo)"
    *
    * Note that this converter includes the class's constructor params in the cache key, where applicable.
    */
  val includeClassConstructorParams = new MethodCallToStringConverter {
    def toString(
        fullClassName: String,
        constructorParamss: IndexedSeq[IndexedSeq[Any]],
        methodName: String,
        paramss: IndexedSeq[IndexedSeq[Any]]
    ): String = {
      val sb = new JStringBuilder(128)
      appendClassNameAndParamsPart(sb)(fullClassName, constructorParamss)
      sb.append(methodName)
      appendParamssPart(sb)(paramss)
      sb.toString
    }
  }

  /** A converter that includes only the method arguments in the cache key. It builds keys of the form: "(arg, ...)(arg,
    * ...)..." e.g. a call to `com.foo.MyClass(42, wow).doSomething(123, abc)(foo)` would be cached as "(123,
    * abc)(foo)".
    *
    * Warning: Do not use this key if you have multiple methods that you want to memoize, because cache keys can
    * collide. e.g. the results of `Foo.bar(123)` and `Baz.wow(123)` would be cached with the same key `123`.
    */
  val onlyMethodParams = new MethodCallToStringConverter {
    def toString(
        fullClassName: String,
        constructorParamss: IndexedSeq[IndexedSeq[Any]],
        methodName: String,
        paramss: IndexedSeq[IndexedSeq[Any]]
    ): String = {
      val sb = new JStringBuilder(128)
      appendParamssPart(sb)(paramss)
      sb.toString
    }
  }

}
