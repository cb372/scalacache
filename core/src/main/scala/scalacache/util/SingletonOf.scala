package scalacache.util

case class SingletonOf[T, U](value: U)

object SingletonOf {
  implicit def mkSingletonOf[T <: AnyRef](implicit t: T): SingletonOf[T, t.type] = SingletonOf(t)
}

