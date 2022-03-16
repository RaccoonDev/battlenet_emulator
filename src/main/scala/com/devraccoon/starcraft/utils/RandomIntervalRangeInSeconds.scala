package com.devraccoon.starcraft.utils

import scala.concurrent.duration._
import scala.util.Random

case class RandomIntervalRangeInSeconds(from: Int, to: Int) {
  private val (start, finish) = if (from < to) (from, to) else (to, from)

  def duration: FiniteDuration =
    if (start == finish) start.seconds
    else (start + Random.nextInt(finish - start)).seconds
}

object RandomIntervalRangeInSeconds {
  def apply(nonRandomValue: Int): RandomIntervalRangeInSeconds =
    RandomIntervalRangeInSeconds(nonRandomValue, nonRandomValue)
}
