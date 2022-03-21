package com.devraccoon.starcraft.utils

import cats.effect.IO
import munit.CatsEffectSuite

class RandomIntervalRangeInSecondsSuit extends CatsEffectSuite {
  def testTwoValues(a: Int, b: Int): IO[Unit] = for {d <- RandomIntervalRangeInSeconds(a, b).duration} yield {
      assert(d.toSeconds >= Math.min(a, b) && d.toSeconds < Math.max(a, b),
            s"expected result to be between $a and $b, but got: ${d.toSeconds}")   
  }

  def testOneValue(a: Int): IO[Unit] = for { d <- RandomIntervalRangeInSeconds(a).duration } yield {
    assert(d.toSeconds == a, s"expected result to be $a, but got ${d.toSeconds}")
  }

  test("random interval between positive values") {
    testTwoValues(1, 2)
  }

  test("random interval between positive value in wrong order") {
    testTwoValues(2, 1)
  }

  test("random interval between same numbers equals to the given number") {
    testOneValue(10)
  }

  test("random interval between one negative and one positive numbers") {
    testTwoValues(-1, 1)
  }

  test("zero interval returns zero") {
    testOneValue(0)
  }
}
