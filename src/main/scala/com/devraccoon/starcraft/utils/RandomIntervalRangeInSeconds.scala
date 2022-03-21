package com.devraccoon.starcraft.utils

import cats.effect.IO
import scala.concurrent.duration._
import scala.util.Random

case class RandomIntervalRangeInSeconds(from: Int, to: Int) {
  private val (start, finish) = if (from < to) (from, to) else (to, from)

  def duration: IO[FiniteDuration] =
    if (start == finish) IO(start.seconds)
    else for {
      duration <- IO(Random.nextInt(finish - start))
    } yield (start + duration).seconds
}

object RandomIntervalRangeInSeconds {
  def apply(nonRandomValue: Int): RandomIntervalRangeInSeconds =
    RandomIntervalRangeInSeconds(nonRandomValue, nonRandomValue)
}
