package com.devraccoon.starcraft

import cats.effect.{IO, IOApp, Ref}
import com.devraccoon.starcraft.domain.server.State
import com.devraccoon.starcraft.utils._
import com.github.javafaker.Faker

object BattleNetEmulator extends IOApp.Simple {

  private val userRegistrationInterval = RandomIntervalRangeInSeconds(10, 30)

  def registerPlayer(serverState: Ref[IO, State], faker: Faker): IO[Unit] =
    for {
      registeredPlayer <- Players.playerRegistration(userRegistrationInterval,
                                                     faker)
      _ <- serverState.update(_.addRegisteredPlayer(registeredPlayer))
    } yield ()

  override def run: IO[Unit] =
    for {
      faker <- IO(new Faker())
      serverState <- Ref[IO].of(State.empty)
      _ <- registerPlayer(serverState, faker)
      state <- serverState.get
      _ <- IO(println(state))
    } yield ()

}
