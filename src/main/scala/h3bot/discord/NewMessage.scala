package h3bot.discord

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

final case class NewMessage(content: String) extends AnyVal

object NewMessage {
  implicit val encoder: Encoder[NewMessage] = deriveEncoder
}
