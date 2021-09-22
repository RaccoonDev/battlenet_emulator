package com.devraccoon.starcraft

import cats.effect.{Clock, IO}
import com.devraccoon.starcraft.domain.player._
import com.devraccoon.starcraft.utils.RandomIntervalRangeInSeconds
import com.github.javafaker.Faker

import java.util.UUID

object Players {

  /**
    * When started this method is going to emit a new user every 10-30 seconds
    */
  def playerRegistration(interval: RandomIntervalRangeInSeconds, faker: Faker)(
      implicit clock: Clock[IO]): IO[RegisterPlayer] =
    for {
      _ <- IO(interval.duration) flatMap IO.sleep
      nickname <- IO(
        Nickname(faker.funnyName().name().toLowerCase().replace(" ", "_")))
      registrationTime <- clock.realTimeInstant
      playerRegisteredEvent <- IO(
        RegisterPlayer(id = PlayerId(UUID.randomUUID()),
                       registrationTime = registrationTime,
                       nickname = nickname))
    } yield playerRegisteredEvent

}
