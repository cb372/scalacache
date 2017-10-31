package scalacache

import monix.eval.Task

object Monix {

  object modes {

    implicit val task: Mode[Task] = new Mode[Task] {
      val M: Async[Task] = CatsEffect.asyncForCatsEffectAsync[Task]
    }

  }

}
