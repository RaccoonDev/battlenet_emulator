package com.devraccoon.starcraft

import cats.effect.kernel.Resource
import cats.effect.{Clock, IO, IOApp, Ref}
import cats.syntax.foldable._
import com.banno.kafka.ClientId
import com.banno.kafka.producer.ProducerApi
import com.devraccoon.starcraft.KafkaOutput.EventId
import com.devraccoon.starcraft.domain.server.{ServerEvent, State}
import com.devraccoon.starcraft.utils._
import com.github.javafaker.Faker
import org.apache.kafka.clients.producer.ProducerRecord

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

  def dispatchEventsToKafka(
      state: Ref[IO, State],
      producerResource: Resource[IO, ProducerApi[IO, EventId, ServerEvent]])
    : IO[Unit] = {
    producerResource.use { p =>
      for {
        events <- dispatchEvents(state)
          .map(events =>
            events.map(e =>
              new ProducerRecord(KafkaOutput.topicName, EventId(e.getId), e)))
        _ <- events.traverse_(p.sendSync)
      } yield ()
    }

  }

  def randomlyBringPlayerOnline(state: Ref[IO, State])(
      implicit clock: Clock[IO]): IO[Unit] =
    for {
      currentTime <- clock.realTimeInstant
      _ <- state.update(_.bringSomePlayersOnline(currentTime))
    } yield ()

  def randomlyBringPlayersOffline(state: Ref[IO, State])(
      implicit clock: Clock[IO]): IO[Unit] =
    for {
      currentTime <- clock.realTimeInstant
      _ <- state.update(_.bringSomePlayersOffline(currentTime))
    } yield ()

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

  def randomlyCompleteSomeGames(state: Ref[IO, State])(
      implicit clock: Clock[IO]): IO[Unit] =
    for {
      currentTime <- clock.realTimeInstant
      _ <- state.update(_.randomlyCompleteSomeGames(currentTime))
    } yield ()

  override def run: IO[Unit] =
    for {
      _ <- KafkaOutput.init
      faker <- IO(new Faker())
      gameMaps <- Maps.getMaps
      serverState <- Ref[IO].of(State.empty.registerGameMaps(gameMaps))
      _ <- registerPlayer(serverState, faker).foreverM.start
      _ <- (IO.sleep(1.seconds) >> randomlyBringPlayerOnline(serverState)).foreverM.start
      _ <- (IO.sleep(1.seconds) >> startLookingForGame(serverState)).foreverM.start
      _ <- (IO.sleep(1.seconds) >> startGamesThatMatches(serverState)).foreverM.start
      _ <- (IO.sleep(5.seconds) >> randomlyCompleteSomeGames(serverState)).foreverM.start
      _ <- (IO.sleep(3.seconds) >> randomlyBringPlayersOffline(serverState)).foreverM.start

      dispatchEventsFib <- (IO.sleep(500.milliseconds) >> dispatchEventsToKafka(
        serverState,
        KafkaOutput
          .getProducer(ClientId("battle-net-server")))).foreverM.start

      _ <- dispatchEventsFib.join
    } yield ()

}
