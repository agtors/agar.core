package com.agar.core.logger

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{handleWebSocketMessages, path}
import akka.http.scaladsl.server.Route
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

  def props(host: String, port: Int)(implicit system: ActorSystem): Props = Props(new Journal(host, port)(system))

  final case class WorldState(players: Map[ActorRef, PlayerState], energies: Map[ActorRef, EnergyState])

}

// A write-ahead logging actor
// The journal is used by the client to update his state
// This is sink with though a websocket connection
// TODO: Store the logs/event in a database with JDBC connector
class Journal(external: String) extends Actor with ActorLogging {

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val route: Route = path("ws") {
    handleWebSocketMessages(WebSocket.listen())
  }

  def this(host: String, port: Int)(system: ActorSystem) {
    this(host + ":" + port)
    Http()(system).bindAndHandle(route, host, port).onComplete {
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
      println("Send to " + external + " " + playersInfos.size + " players, " + energiesInfos.size + " energies")
      WebSocket.sendText(Json.arr(playersInfos.asJson, energiesInfos.asJson).noSpaces)
  }
}
