package com.devraccoon.starcraft

import cats.effect.{Clock, IO, IOApp, Ref}
import com.devraccoon.starcraft.domain.server.State
import com.devraccoon.starcraft.utils._
import com.github.javafaker.Faker

import scala.concurrent.duration._

object BattleNetEmulator extends IOApp.Simple {

  private val userRegistrationInterval = RandomIntervalRangeInSeconds(10, 30)

  def registerPlayer(serverState: Ref[IO, State], faker: Faker): IO[Unit] =
    for {
      registeredPlayer <- Players.playerRegistration(userRegistrationInterval,
                                                     faker)
      _ <- serverState.update(_.addRegisteredPlayer(registeredPlayer))
    } yield ()

  def printStateEveryTenSeconds(state: Ref[IO, State]): IO[Unit] =
    IO.sleep(10.seconds) >> state.get.map(s => println(s)) >> printStateEveryTenSeconds(
      state)

  // After registration players are randomly scheduled from offline to online
  def randomlyBringPlayerOnline(state: Ref[IO, State])(
      implicit clock: Clock[IO]): IO[Unit] =
    for {
      currentTime <- clock.realTimeInstant
      _ <- state.update(_.bringSomePlayersOnline(currentTime))
    } yield ()

  def startLookingForGame(state: Ref[IO, State])(
      implicit clock: Clock[IO]): IO[Unit] =
    for {
      currentTime <- clock.realTimeInstant
      _ <- state.update(_.startLookingForGame(currentTime))
    } yield ()

  // With some random interval the system takes an online player and starts a game search
  // with random waiting interval. Type of a game and can is picked randomly.

  // Another thread checks the server state every minutes to see if a game should be started

  override def run: IO[Unit] =
    for {
      faker <- IO(new Faker())
      serverState <- Ref[IO].of(State.empty)
      _ <- registerPlayer(serverState, faker).foreverM.start
      _ <- (IO.sleep(5.seconds) >> randomlyBringPlayerOnline(serverState)).foreverM.start
      _ <- (IO.sleep(4.seconds) >> startLookingForGame(serverState)).foreverM.start
      printStateFib <- printStateEveryTenSeconds(serverState).start
      _ <- printStateFib.join
    } yield ()

}
