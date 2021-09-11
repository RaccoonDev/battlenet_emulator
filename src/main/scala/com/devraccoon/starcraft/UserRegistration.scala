package com.devraccoon.starcraft

import cats.effect.IO

import scala.concurrent.duration._
import scala.util.Random

object UserRegistration {

  /**
   * When started this method is going to emit a new user every 10-30 seconds
   */
  def usersRegistrationProcess: IO[Unit] = for {
    _ <- IO((10 + Random.nextInt(21)).seconds) flatMap IO.sleep
    _ <- IO(println("new user registered"))
  } yield()

}
