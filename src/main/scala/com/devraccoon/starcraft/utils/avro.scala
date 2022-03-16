package com.devraccoon.starcraft.utils

import cats.data.NonEmptyVector
import com.devraccoon.starcraft.domain.game.{GameId, RegionId}
import com.devraccoon.starcraft.domain.maps.MapId
import com.devraccoon.starcraft.domain.player.{Nickname, PlayerId}
import com.sksamuel.avro4s.{Decoder, Encoder, FieldMapper, SchemaFor}
import org.apache.avro.{LogicalType, Schema}

import java.util.UUID

object avro {

  implicit object PlayerIdSchemaFor extends SchemaFor[PlayerId] {
    override def schema(fieldMapper: FieldMapper): Schema = {
      new LogicalType("UUID").addToSchema(Schema.create(Schema.Type.STRING))
    }
  }

  implicit object PlayerIdEncoder extends Encoder[PlayerId] {
    override def encode(t: PlayerId,
                        schema: Schema,
                        fieldMapper: FieldMapper): AnyRef = t.value.toString
  }

  implicit object PlayerIdDecoder extends Decoder[PlayerId] {
    override def decode(value: Any,
                        schema: Schema,
                        fieldMapper: FieldMapper): PlayerId =
      PlayerId(UUID.fromString(value.toString))
  }

  implicit object GameIdSchemaFor extends SchemaFor[GameId] {
    override def schema(fieldMapper: FieldMapper): Schema = {
      new LogicalType("UUID").addToSchema(Schema.create(Schema.Type.STRING))
    }
  }

  implicit object GameIdEncoder extends Encoder[GameId] {
    override def encode(t: GameId,
                        schema: Schema,
                        fieldMapper: FieldMapper): AnyRef = t.value.toString
  }

  implicit object GameIdDecoder extends Decoder[GameId] {
    override def decode(value: Any,
                        schema: Schema,
                        fieldMapper: FieldMapper): GameId =
      GameId(UUID.fromString(value.toString))
  }

  implicit object RegionIdSchemaFor extends SchemaFor[RegionId] {
    override def schema(fieldMapper: FieldMapper): Schema = {
      new LogicalType("UUID").addToSchema(Schema.create(Schema.Type.STRING))
    }
  }

  implicit object RegionIdEncoder extends Encoder[RegionId] {
    override def encode(t: RegionId,
                        schema: Schema,
                        fieldMapper: FieldMapper): AnyRef = t.value.toString
  }

  implicit object RegionIdDecoder extends Decoder[RegionId] {
    override def decode(value: Any,
                        schema: Schema,
                        fieldMapper: FieldMapper): RegionId =
      RegionId(UUID.fromString(value.toString))
  }

  implicit object NicknameSchemaFor extends SchemaFor[Nickname] {
    override def schema(fieldMapper: FieldMapper): Schema = {
      Schema.create(Schema.Type.STRING)
    }
  }

  implicit object NicknameEncoder extends Encoder[Nickname] {
    override def encode(t: Nickname,
                        schema: Schema,
                        fieldMapper: FieldMapper): AnyRef = t.value
  }

  implicit object NicknameDecoder extends Decoder[Nickname] {
    override def decode(value: Any,
                        schema: Schema,
                        fieldMapper: FieldMapper): Nickname =
      Nickname(value.toString)
  }

  implicit object MapIdSchemaFor extends SchemaFor[MapId] {
    override def schema(fieldMapper: FieldMapper): Schema = {
      Schema.create(Schema.Type.STRING)
    }
  }

  implicit object MapIdEncoder extends Encoder[MapId] {
    override def encode(t: MapId,
                        schema: Schema,
                        fieldMapper: FieldMapper): AnyRef = t.value
  }

  implicit object MapIdDecoder extends Decoder[MapId] {
    override def decode(value: Any,
                        schema: Schema,
                        fieldMapper: FieldMapper): MapId =
      MapId(value.toString)
  }

  implicit def nonEmptyVectorSchemaFor[T](
      implicit schemaFor: SchemaFor[T]): SchemaFor[NonEmptyVector[T]] =
    (fieldMapper: FieldMapper) =>
      Schema.createArray(schemaFor.schema(fieldMapper))

  implicit def nonEmptyVectorEncoder[T](
      implicit encoder: Encoder[T]): Encoder[NonEmptyVector[T]] =
    (t: NonEmptyVector[T], schema: Schema, fieldMapper: FieldMapper) =>
      Encoder[Vector[T]].encode(t.toVector, schema, fieldMapper)

  implicit def nonEmptyVectorDecoder[T](
      implicit decoder: Decoder[T]): Decoder[NonEmptyVector[T]] =
    (value: Any, schema: Schema, fieldMapper: FieldMapper) =>
      NonEmptyVector.fromVectorUnsafe(
        Decoder[Vector[T]].decode(value, schema, fieldMapper))

}
