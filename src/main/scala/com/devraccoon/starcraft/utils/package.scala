package com.devraccoon.starcraft

import java.time.Instant
import scala.util.Random

package object utils {
  implicit class IntOps(v: Int) {
    def percents(p: Int): Int = (v * (p.toDouble / 100)).toInt
  }

  implicit class ListOps[+A](l: Seq[A]) {
    def takeRandomElement(count: Int): Seq[A] =
      Random.shuffle(l).take(count)

    def takeRandomPercentOfElements(percents: Int): Seq[A] =
      Random.shuffle(l).take(l.length.percents(percents))

    def takeOneRandomElement(): Option[A] =
      Random.shuffle(l).headOption
  }

  implicit class InstantOps(i: Instant) {
    def millisPrecision: Instant = Instant.ofEpochMilli(i.toEpochMilli)
  }

}
