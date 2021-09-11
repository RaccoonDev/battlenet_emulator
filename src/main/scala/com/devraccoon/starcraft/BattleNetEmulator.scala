package com.devraccoon.starcraft

import cats.effect.{IO, IOApp}

object BattleNetEmulator extends IOApp.Simple {

  override def run: IO[Unit] = UserRegistration.usersRegistrationProcess.foreverM

}
