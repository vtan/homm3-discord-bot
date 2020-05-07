package h3bot

import h3bot.bot.BotFlow
import h3bot.discord.DiscordApiClient

import scala.concurrent.Future
import scala.util.{Failure, Success}

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import sttp.client._
import sttp.client.akkahttp.AkkaHttpBackend

object Main {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: Materializer = Materializer(system)
    import system.dispatcher
    implicit val sttpBackend: SttpBackend[Future, Nothing, NothingT] = AkkaHttpBackend()

    val config = ConfigFactory.load()
    val discordApiClient = new DiscordApiClient(config)
    val creatures = CreatureListScraper.scrapeCreatures
    val botFlow = new BotFlow(config, discordApiClient, creatures)

    val (cancellable, done) = botFlow.runnableGraph.run
    done.onComplete { result =>
      result match {
        case Success(_) => system.log.info("The bot flow completed successfully")
        case Failure(ex) => system.log.error("The bot flow completed with a failure", ex)
      }
      sttpBackend.close.flatMap(_ => system.terminate)
    }

    System.console match {
      case null => ()
      case console =>
        system.log.info("Running with terminal input, enter a line to terminate")
        console.readLine()
        system.log.info("Terminating")
        val _ = cancellable.cancel()
    }
  }
}

