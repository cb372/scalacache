package scalacache

import monix.eval.Task

object Monix {

  object implicits {

    implicit final val task: Async[Task] = CatsEffect.asyncForCatsEffectAsync[Task]

  }

}
