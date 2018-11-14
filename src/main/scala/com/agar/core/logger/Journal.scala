package com.agar.core.logger

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{handleWebSocketMessages, path}
import akka.stream.ActorMaterializer
import com.agar.core.gameplay.player.deco._
import com.agar.core.gameplay.player.{EnergyInfos, PlayerInfos}
import com.agar.core.logger.Journal.WorldState
import com.agar.core.region.State.{EnergyState, PlayerState}
import com.agar.core.utils.WebSocket
import io.circe._
import io.circe.syntax._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Journal {

  def props(implicit system: ActorSystem): Props = Props(new Journal(system))

  final case class WorldState(players: Map[ActorRef, PlayerState], energies: Map[ActorRef, EnergyState])

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
    Http()(system).bindAndHandle(route, "127.0.0.1", 8000).onComplete {
      case Success(binding) => log.info(s"Listening on ${binding.localAddress.getHostString}:${binding.localAddress.getPort}.")
      case Failure(exception) => throw exception
    }
  }

  override def receive: Receive = {
    case WorldState(players, energies) =>
      // please forgive me...
      // TODO: We can send raw binary value instead of serialize into a JSON file
      val playersInfos = players.map { case (ref, state) => PlayerInfos(state.position, state.velocity, state.weight, ref) }.toList
      val energiesInfos = energies.map { case (ref, state) => EnergyInfos(state.position, state.value, ref) }.toList

      // TODO: We can switch to BinaryMessage to increase the speed
      WebSocket.sendText(Json.arr(playersInfos.asJson, energiesInfos.asJson).noSpaces)
  }
}
