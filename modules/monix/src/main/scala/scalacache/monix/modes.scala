package scalacache.monix

import monix.eval.Task

import scalacache.cats.effect.CatsEffectInstances
import scalacache.{Async, Mode}

object modes {

  implicit val task: Mode[Task] = new Mode[Task] {
    val M: Async[Task] = CatsEffectInstances.asyncForCatsEffectAsync[Task]
  }

}
