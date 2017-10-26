package scalacache.scalaz72

import scalacache.{Async, Mode}
import scalaz.concurrent.Task

object modes {

  implicit val task: Mode[Task] = new Mode[Task] {
    val M: Async[Task] = ScalazInstances.AsyncForScalazTask
  }

}
