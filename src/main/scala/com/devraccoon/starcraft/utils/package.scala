package com.devraccoon.starcraft

import scala.util.Random

package object utils {
  implicit class IntOps(v: Int) {
    def percents(p: Int): Int = v * (p / 100)
  }

  implicit class ListOps[A](l: List[A]) {
    def takeRandomElement(count: Int): List[A] =
      Random.shuffle(l).take(count)

    def takeRandomPercentOfElements(percents: Int): List[A] =
      Random.shuffle(l).take(l.length.percents(percents))
  }
}
