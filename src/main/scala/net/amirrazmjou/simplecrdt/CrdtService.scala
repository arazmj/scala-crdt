package net.amirrazmjou.simplecrdt

import java.io.PrintWriter

import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.json.{DefaultJsonProtocol, _}
import spray.routing._

import scala.util.{Try}
import scala.concurrent.{Await, Future}
import spray.client.pipelining._
import spray.http.HttpRequest
import spray.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import akka.event.Logging

case class Config(actors: List[String])

object Config extends DefaultJsonProtocol {
  implicit val configFormat = jsonFormat1(Config.apply)
}

// this trait defines our service behavior independently from the service actor
trait CrdtService extends HttpService {
  import scala.collection.mutable.HashMap

  val fileNameActors = s"${Boot.id}_actors.json"
  val fileMap = s"${Boot.id}_map.json"

  def readActors(): List[String] = {
    Try {
      val source = scala.io.Source.fromFile(fileNameActors)
      val lines = try source.mkString finally source.close()
      return lines.parseJson.convertTo[Config].actors
    }
    List()
  }

  def writeActors(actors: Config): Unit = {
    Try {
        new PrintWriter(fileNameActors)
        {
          write(actors.toJson.prettyPrint)
          close
        }
      }
  }

  def readMap(): HashMap[String, Int] = {
    // If the counter does not exist, respond with 0.
    val map = new HashMap[String, Int]()
          { override def default(key:String) = 0 }

    Try {
      scala.io.Source.fromFile(fileMap).getLines.foreach(line => {
        line.split("\t") match {
          case Array(n, v) => {
            val name = n.trim
            val value = Integer.parseInt(v.trim)
            map += (name -> value)
          }
          case _ =>
        }
      })
    }
    map
  }

  def writeMap(map: HashMap[String,Int]): Unit  = {
    new PrintWriter(fileMap) {
      map.foreach {
        case (name, value) => write(s"$name\t$value\n")
      }
      close
    }
  }

  def consistent_value(name: String) : Integer = {
    val port = Boot.port
    for (actor <- actors) {
        Try {
          val fResponse: Future[HttpResponse] = pipeline(Get(s"http://$actor:$port/$name/value"))
          val response = Await.result(fResponse, Duration("5 seconds"))
          if (response.status.isSuccess) {
            val newValue = Integer.parseInt(response.entity.asString)
            if (newValue < map(name)) {
              // update remote actor and ignore the result
              pipeline(Post(s"http://$actor:$port/$name", map(name).toString))
            } else if (newValue > map(name)) {
              // update our value
              map += { name -> newValue }
            }
          }
        }
    }
    map(name)
  }

  // pipline for call other actors endpoints
  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive
  // map to store keyvalues
  var map = readMap()
  var actors:List[String] = readActors()

  val route =
    path("config") {
      post {
          entity(as[Config]) { config => {
            complete {
              actors = config.actors;
              writeActors(config)
              StatusCodes.OK
            }
          }
        }
      }
    } ~
    pathPrefix("counter" / Segment) {
      (name) => {
        get {
          pathPrefix("value") {
            pathEnd {
              complete {
                map(name).toString
              }
            }
          } ~
            pathPrefix("consistent_value") {
              pathEnd {
                complete {
                  consistent_value(name).toString
                }
              }
            }
        } ~
          post {
            pathEnd {
              entity(as[String]) {
                value => {
                  map += {name -> Integer.parseInt(value)}
                  writeMap(map)
                  complete(StatusCodes.OK)
                }
              }
            }
          }
      }
    }
}