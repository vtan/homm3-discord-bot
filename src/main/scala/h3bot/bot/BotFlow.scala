package h3bot.bot

import h3bot.CreatureList
import h3bot.discord.{DiscordApiClient, Message, NewMessage}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import akka.Done
import akka.actor.Cancellable
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source}
import com.typesafe.config.Config
import org.slf4j.{Logger, LoggerFactory}

class BotFlow(
  config: Config,
  discordApiClient: DiscordApiClient,
  creatureList: CreatureList
)(
  implicit ec: ExecutionContext
) {
  private val channelId: String = config.getString("bot.channelId")

  private val log: Logger = LoggerFactory.getLogger(this.getClass)

  val runnableGraph: RunnableGraph[(Cancellable, Future[Done])] =
    messageSource
      .filter(_.content startsWith "!h3")
      .mapConcat[String] { message =>
        val queriedName = message.content.stripPrefix("!h3").trim
        creatureList.search(queriedName).map(_.toString).toList
      }
      .map(NewMessage(_))
      .toMat(replySink)(Keep.both)

  private def messageSource: Source[Message, Cancellable] = {
    final case class State(lastMessageId: Option[String])
    val initialState = State(lastMessageId = None)

    val tickSource = Source.tick(initialDelay = 0.seconds, interval = 1.second, tick = ())

    val requestSource = Source.unfoldAsync(initialState) { state =>
      discordApiClient.getMessages(channelId, state.lastMessageId)
        .map { messages =>
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
        .recover {
          case ex =>
            log.error("Failed to fetch messages", ex)
            Some((initialState, Vector.empty))
        }
    }

    (tickSource zip requestSource).mapConcat {
      case (_, messagesByTimeDescending) => messagesByTimeDescending.reverse
    }
  }

  private def replySink: Sink[NewMessage, Future[Done]] =
    Sink.foreachAsync(parallelism = 1)(
      // TODO rate limiting
      discordApiClient.createMessage(channelId, _)
        .recover { case ex => log.error("Failed to create message", ex) }
    )
}
