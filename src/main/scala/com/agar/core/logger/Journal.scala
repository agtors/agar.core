package com.agar.core.logger

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives.{handleWebSocketMessages, path}
import com.agar.core.logger.Journal.SetEntitiesState
import com.agar.core.utils.WebSocket
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Journal {

  def props(implicit system: ActorSystem): Props = Props(new Journal(system))

  final case object SetEntitiesState
}

// A write-ahead logging actor
// The journal is used by the client to update his state
// This is sink with though a websocket connection
// TODO: Store the logs/event in a database with JDBC connector
class Journal extends Actor with ActorLogging {

  implicit val materializer = ActorMaterializer()

  val route = path("ws") {
    handleWebSocketMessages(WebSocket.listen())
  }

  def this(system: ActorSystem) {
    this()
    Http()(system).bindAndHandle(route, "127.0.0.1", 4200).onComplete {
      case Success(binding)   => log.info(s"Listening on ${binding.localAddress.getHostString}:${binding.localAddress.getPort}.")
      case Failure(exception) => throw exception
    }
  }

  override def receive: Receive = {
    case SetEntitiesState =>
      WebSocket.sendText("ping")
  }
}
