package h3bot

import h3bot.discord.{Message, NewMessage}

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.{Done, NotUsed}
import sttp.client._
import sttp.client.akkahttp._
import sttp.client.circe._

object Main {

  def main(args: Array[String]): Unit = {
    val token = Option(System.getenv("TOKEN")).filter(_.nonEmpty) match {
      case Some(t) => t
      case None => throw new Exception("No TOKEN enviroment variable")
    }
    val channelId = "706614311056834622"

    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: Materializer = Materializer(system)
    import system.dispatcher

    val creatures = CreatureListScraper.scrapeCreatures

    implicit val sttpBackend = AkkaHttpBackend()

    final case class State(lastMessageId: Option[String])

    val messageSource: Source[Message, NotUsed] = {
      val tickSource = Source.tick(initialDelay = 0.seconds, interval = 1.second, tick = ())
      val requestSource = Source
        .unfoldAsync(State(lastMessageId = None)) { state =>
          basicRequest
            .get(uri"https://discordapp.com/api/channels/$channelId/messages?after=${state.lastMessageId}")
            .header("Authorization", s"Bot $token")
            .header("User-Agent", "DiscordBot")
            .response(asJson[Vector[Message]])
            .send()
            .map { response =>
              val messages = response.body match {
                case Right(messages) => messages
                case Left(error) => throw error // TODO error handling
              }
              val newState = messages match {
                case mostRecentMessage +: _ => state.copy(lastMessageId = Some(mostRecentMessage.id))
                case _ => state
              }
              val messagesToEmit = if (state.lastMessageId.isDefined) {
                messages
              } else {
                // The message was sent before we started listening, so we won't reply.
                Vector.empty
              }
              Some((newState, messagesToEmit))
            }
        }
      (tickSource zip requestSource)
        .mapConcat(_._2.reverse)
        .mapMaterializedValue(_ => NotUsed)
    }

    val replySink: Sink[String, Future[Done]] = Sink.foreachAsync(parallelism = 1) { reply =>
      val newMessage = NewMessage(reply)
      basicRequest
        .post(uri"https://discordapp.com/api/channels/$channelId/messages")
        .header("Authorization", s"Bot $token")
        .header("Content-Type", "application/json")
        .header("User-Agent", "DiscordBot")
        .body(newMessage)
        .send()
        .map { response =>
          // TODO rate limiting
          system.log.info(s"POST headers: ${response.headers}")
          if (!response.code.isSuccess) {
            system.log.error(s"Failed to create message: ${response.statusText} ${response.body}")
          }
        }
    }

    val _ = messageSource
      .filter(_.content startsWith "!h3")
      .mapConcat[String] { message =>
        val queriedName = message.content.stripPrefix("!h3").trim
        creatures
          .find(
            _.name.toLowerCase.filterNot(_.isSpaceChar) ==
              queriedName.toLowerCase.filterNot(_.isSpaceChar)
          )
          .map(_.toString)
          .toList
      }
      .to(replySink)
      .run()

    System.console match {
      case null => ()
      case console =>
        system.log.info("Running with terminal input, enter a line to terminate")
        console.readLine()
        system.log.info("Terminating")
        val _ = system.terminate()
    }
  }
}

