package com.devraccoon.starcraft

import com.devraccoon.starcraft.domain.player.PlayerId
import com.devraccoon.starcraft.domain.server.ServerEvent
import com.sksamuel.avro4s.AvroSchema
import munit.FunSuite
import com.devraccoon.starcraft.utils.avro._
import org.junit.Assert._

class AvroSuite extends FunSuite {

  test("Avro Schema for PlayerId is UUID") {
    val schema = AvroSchema[PlayerId]
    println(schema.toString(true))
    assertNotNull(schema.toString)
  }

  test("Avro Schema for Server Events is not null") {
    val schema = AvroSchema[ServerEvent]
    val stringSchema = schema.toString
    println(schema.toString(true))
    assertNotNull(stringSchema)
  }

}
