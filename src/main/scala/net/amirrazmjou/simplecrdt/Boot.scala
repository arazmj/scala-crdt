package net.amirrazmjou.simplecrdt

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http

import scala.concurrent.duration._

object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("simple-crdt")
  val port = 7777
  val id = args.headOption.getOrElse(throw new RuntimeException("No id parameter is provided."))

  // create and start our service actor
  val service = system.actorOf(Props[CrdtServiceActor], "crdt-service")

  implicit val timeout = Timeout(5.seconds)
  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = "localhost", port = port)
}
