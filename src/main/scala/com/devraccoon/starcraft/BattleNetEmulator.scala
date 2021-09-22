package com.devraccoon.starcraft

import cats.effect.{Clock, IO, IOApp, Ref}
import com.devraccoon.starcraft.domain.server.{ServerEvent, State}
import com.devraccoon.starcraft.utils._
import com.github.javafaker.Faker

import scala.concurrent.duration._

object BattleNetEmulator extends IOApp.Simple {

  private val userRegistrationInterval = RandomIntervalRangeInSeconds(1, 3)

  def registerPlayer(serverState: Ref[IO, State], faker: Faker): IO[Unit] =
    for {
      registeredPlayer <- Players.playerRegistration(userRegistrationInterval,
                                                     faker)
      _ <- serverState.update(_.addRegisteredPlayer(registeredPlayer))
    } yield ()

  def dispatchEvents(state: Ref[IO, State]): IO[List[ServerEvent]] =
    state.modify(_.dispatchAllEvents)

  def dispatchEventsToStdOut(state: Ref[IO, State]): IO[Unit] =
    dispatchEvents(state).map(l => l.foreach(println))

  // After registration players are randomly scheduled from offline to online
  def randomlyBringPlayerOnline(state: Ref[IO, State])(
      implicit clock: Clock[IO]): IO[Unit] =
    for {
      currentTime <- clock.realTimeInstant
      _ <- state.update(_.bringSomePlayersOnline(currentTime))
    } yield ()

  // With some random interval the system takes an online player and starts a game search
  def startLookingForGame(state: Ref[IO, State])(
      implicit clock: Clock[IO]): IO[Unit] =
    for {
      currentTime <- clock.realTimeInstant
      _ <- state.update(_.startSomePlayersLookingForGame(currentTime))
    } yield ()

  def startGamesThatMatches(state: Ref[IO, State])(
      implicit clock: Clock[IO]): IO[Unit] =
    for {
      currentTime <- clock.realTimeInstant
      _ <- state.update(_.startMatchedGames(currentTime))
    } yield ()

  // games are started one by one randomly

  // Another thread checks the server state every minutes to see if a game should be started

  override def run: IO[Unit] =
    for {
      faker <- IO(new Faker())
      gameMaps <- Maps.getMaps
      serverState <- Ref[IO].of(State.empty.registerGameMaps(gameMaps))
      _ <- registerPlayer(serverState, faker).foreverM.start
      _ <- (IO.sleep(1.seconds) >> randomlyBringPlayerOnline(serverState)).foreverM.start
      _ <- (IO.sleep(1.seconds) >> startLookingForGame(serverState)).foreverM.start
      _ <- (IO.sleep(1.seconds) >> startGamesThatMatches(serverState)).foreverM.start

      dispatchEventsFib <- (IO.sleep(500.milliseconds) >> dispatchEventsToStdOut(
        serverState)).foreverM.start

      _ <- dispatchEventsFib.join
    } yield ()

}
