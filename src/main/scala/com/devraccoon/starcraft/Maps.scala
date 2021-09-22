package com.devraccoon.starcraft

import cats.effect.IO
import com.devraccoon.starcraft.domain.maps.{
  GameMap,
  MapId,
  MapName,
  MaxPlayers,
  MinPlayers
}

import scala.util.Random

object Maps {
  def getMaps: IO[Vector[GameMap]] = IO.pure(
    Vector(
      GameMap(
        MapId("list_tample"),
        MapName("Lost Tample"),
        MaxPlayers(4),
        MinPlayers(2)
      ))
  )

  def getRandomMap: IO[GameMap] =
    for {
      maps <- getMaps
    } yield maps(Random.nextInt(maps.size))
}
