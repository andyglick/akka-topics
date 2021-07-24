package example.http

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ MissingCookieRejection, Route }
import scala.io.StdIn

object HttpServerReject {

  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem(Behaviors.empty, "simple-api")

    implicit val executionContext = system.executionContext

    val route: Route =
      path("ping") {
        get {
          complete(
            HttpResponse(
              200,
              entity =
                HttpEntity(ContentTypes.`application/json`, "pong")))
        } ~
        post {
          reject(
            MissingCookieRejection(
              "to post you have to give me a cookie"))
        }
      }

    val bindingFuture =
      Http().newServerAt("localhost", 8080).bind(route)

    println(s"server at localhost:8080 \nPress RETURN to stop")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}