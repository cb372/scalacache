package com.github.cb372.cache.guava

import org.joda.time.DateTime

/**
 *
 * Author: c-birchall
 * Date:   13/11/14
 */
case class Entry[+A](value: A, expiresAt: Option[DateTime]) {

  def isExpired: Boolean = expiresAt.exists(_.isBeforeNow)

}
