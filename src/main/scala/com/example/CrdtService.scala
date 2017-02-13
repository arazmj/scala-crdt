package net.amirrazmjou.simplecrdt

import java.io.PrintWriter
import spray.http.StatusCodes
import spray.routing._
import spray.json.DefaultJsonProtocol
import spray.httpx.SprayJsonSupport._
import spray.json._
import scala.util.Try
import spray.http._
import spray.client.pipelining._

case class Config(actors: List[String])

object Config extends DefaultJsonProtocol {
  implicit val configFormat = jsonFormat1(Config.apply)
}


// this trait defines our service behavior independently from the service actor
trait CrdtService extends HttpService {
  def readActors(file: String): List[String] = {
    Try {
      val source = scala.io.Source.fromFile(file)
      val lines = try source.mkString finally source.close()
      return lines.parseJson.convertTo[Config].actors
    }
    List()
  }

  def writeActors(file: String, actors: Config): Unit = {
      Try {
        new PrintWriter(file)
        {
          write(actors.toJson.prettyPrint)
          close
        }
      }
  }

  def readMap(fname: String):Map[String,Int] = {
    val map = new scala.collection.mutable.HashMap[String, Int]()
    Try {
      scala.io.Source.fromFile(fname).getLines.foreach(line => {
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
    new scala.collection.immutable.HashMap() ++ map
  }

  def writeMap(file: String, map: Map[String,Int]): Unit  = {
    new PrintWriter(file) {
      map.foreach {
        case (name, value) => write(s"$name\t$value\n")
      }
      close
    }
  }

  var map = readMap("map.json")
  var actors:List[String] = readActors("actors.json")

  val route =
    path("config") {
      post {
          entity(as[Config]) { config => {
            complete {
              actors = config.actors;
              System.out.println(s"Config recevied" + config.toJson.prettyPrint)
              writeActors("actors.json", config)
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
              complete(map(name).toString)
            }
          } ~
            pathPrefix("consistent_value") {
              pathEnd {
                complete(map(name).toString)
              }
            }
        } ~
          post {
            pathEnd {
              entity(as[String]) {
                value => {
                  map = map + {name -> Integer.parseInt(value)}
                  writeMap("map.json", map)
                  complete(s"This is new value for $name is $map(name)")
                }
              }
            }
          }
      }
    }
}