package edgar

import scala.util.{Success, Failure}
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.pattern.ask
//import akka.event.Logging
import com.typesafe.scalalogging.{LazyLogging, Logger}
import akka.io.IO
import spray.json.{JsonFormat, DefaultJsonProtocol}
import spray.can.Http
import spray.httpx.SprayJsonSupport
import spray.client.pipelining._
import spray.util._
import scala.xml.XML


object CompanyForms extends LazyLogging {
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("simple-spray-client")
  import system.dispatcher // execution context for futures below

  def Invoke(cik:String, count:Int=100, formType:String="13F") {
    logger.debug("Calling into edgar for cik: "+cik)

    import ElevationJsonProtocol._
    import SprayJsonSupport._
    val pipeline = sendReceive ~> unmarshal[String]

    val responseFuture = pipeline {
      Get("http://www.sec.gov/cgi-bin/browse-edgar?action=getcompany&"+
        "CIK="+cik+
        "&type="+formType+
        "%25&dateb=&owner=exclude&start=0&count="+
        count+"&output=atom")
    }
    responseFuture onComplete {

      case Success(s) =>
        logger.debug("The Edgar call:\n"+ s)
        val x = scala.xml.XML.loadString(s)
        val a = (x \\ "feed" \ "author" \ "email").text
        shutdown()

      case Failure(error) =>
        logger.error("Couldn't get request. Error: "+error)
        shutdown()
    }
  }

  def shutdown(): Unit = {
    IO(Http).ask(Http.CloseAll)(1.second).await
    system.shutdown()
  }
}