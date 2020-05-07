package h3bot.discord

import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.config.Config
import io.circe.Json
import sttp.client._
import sttp.client.circe._

class DiscordApiClient(
  config: Config
)(
  implicit sttpBackend: SttpBackend[Future, Nothing, NothingT],
  ec: ExecutionContext
) {
  private val apiUrl = config.getString("discord.apiUrl")
  private val botToken = config.getString("discord.botToken")
  private val userAgent = config.getString("discord.userAgent")

  def getMessages(channelId: String, after: Option[String]): Future[Vector[Message]] =
    basicRequest
      .get(uri"$apiUrl/channels/$channelId/messages?after=$after")
      .header("Authorization", s"Bot $botToken")
      .header("User-Agent", userAgent)
      .response(asJson[Vector[Message]])
      .send
      .flatMap(response => Future.fromTry(response.body.toTry))

  def createMessage(channelId: String, newMessage: NewMessage): Future[Unit] =
    basicRequest
      .post(uri"$apiUrl/channels/$channelId/messages")
      .header("Authorization", s"Bot $botToken")
      .header("User-Agent", userAgent)
      .header("Content-Type", "application/json")
      .body(newMessage)
      .response(asJson[Json])
      .send()
      .flatMap(response => Future.fromTry(response.body.toTry).map(_ => ()))
}
