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
import spray.client.pipelining._
import spray.util._
import scala.xml.{Elem, XML}

case class ReqConfig(cik:String,
                      count:Int=10,
                      ftype:String="",
                      dateb:String="",
                      owner:String="",
                      start:Int = 0,
                      output:String="atom",
                      url:String="http://www.sec.gov/cgi-bin/browse-edgar?action=getcompany"){
  override def toString:String ={
    url+
    "&CIK="+cik+
    "&type="+ftype+
    "&dateb="+dateb+
    "&owner="+owner+
    "&start="+start+
    "&count="+count+
    "&output="+output
  }
}

case class XmlForm(xml: Elem){
  val feed = xml \\ "feed"
  val author = feed \ "author"
  val companyInfo = feed \ "company-info"
  val name = companyInfo \ "conformed-name"
  val cik = companyInfo \ "cik"
  val entries = feed \ "entry"
  override def toString:String = "name=\""+name.text+"\"\t cik="+cik.text+"\tentries="+entries.toList.size
}

object FormsWeb extends LazyLogging {
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("edgar-spray-client")
  import system.dispatcher // execution context for futures below

  def Invoke(cik:String, count:Int=10, formType:String="13F") {
    logger.debug("Calling into edgar for cik: "+cik)

    val pipeline = sendReceive ~> unmarshal[String]

    val responseFuture = pipeline {
      val r = ReqConfig(cik, count, formType)
      logger.debug("GET: "+r.toString)
      Get(r toString)
    }
    responseFuture onComplete {

      case Success(s) =>
        //logger.debug("The Edgar call:\n"+ s)
        val form = XmlForm(scala.xml.XML.loadString(s))
        logger.debug("Fetched: "+form.toString)
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