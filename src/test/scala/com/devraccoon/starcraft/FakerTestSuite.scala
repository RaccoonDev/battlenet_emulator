package com.devraccoon.starcraft

import com.github.javafaker.Faker
import munit.FunSuite

class FakerTestSuite extends FunSuite {
  test("faker library generates funny name") {
    val faker = new Faker
    val funnyName = faker.funnyName().name()
    println(funnyName)
    assertNotEquals(funnyName, "")
  }
}
