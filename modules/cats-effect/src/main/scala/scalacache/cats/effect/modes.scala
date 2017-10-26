package scalacache.cats.effect

import cats.effect.IO

import scalacache.{Async, Mode}

object modes {

  /**
    * A mode that wraps computations in cats-effect IO.
    */
  implicit val io: Mode[IO] = new Mode[IO] {
    val M: Async[IO] = CatsEffectInstances.asyncForCatsEffectAsync[IO]
  }

}
