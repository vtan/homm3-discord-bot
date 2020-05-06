package h3bot

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory

object Main {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: Materializer = Materializer(system)
    import system.dispatcher

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

