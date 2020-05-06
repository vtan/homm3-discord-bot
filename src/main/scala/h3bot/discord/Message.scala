package h3bot.discord

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class Message(
  id: String,
  content: String
)

object Message {
  implicit val decoder: Decoder[Message] = deriveDecoder
}
