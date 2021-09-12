package com.devraccoon.starcraft.utils

import munit.FunSuite

class RandomIntervalRangeInSecondsSuit extends FunSuite {
  def testTwoValues(a: Int, b: Int): Unit = {
    val randomInterval = RandomIntervalRangeInSeconds(a, b).duration.toSeconds
    assert(randomInterval >= Math.min(a, b) && randomInterval < Math.max(a, b),
           s"expected result to be between $a and $b, but got: $randomInterval")
  }

  def testOneValue(a: Int): Unit = {
    val r = RandomIntervalRangeInSeconds(a).duration.toSeconds
    assert(r == a, s"expected result to be $a, but got $r")
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
