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
import scala.xml.{Node, Elem, XML}

case class ReqConfig(cik:String,
                      start:Int = 0,
                      dateb:String="",
                      ftype:String="",
                      owner:String="include",
                      count:Int=10,
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

case class XmlForm(entry:Node){
  val content = entry \ "content"
  val filingDate = content \ "filing-date"
  override def toString =
  "filing date:"+filingDate.text
}

case class XmlFormCollection(xml: Elem){
  val feed = xml \\ "feed"
  val author = feed \ "author"
  val companyInfo = feed \ "company-info"
  val name = companyInfo \ "conformed-name"
  val cik = companyInfo \ "cik"
  val entries = feed \ "entry"
  override def toString:String = "name:\""+name.text+
    "\"\tcik:"+cik.text+
    "\tentries:"+entries.toList.size+
    "\tlast:"+(if(!entries.isEmpty){XmlForm(entries.last)}else{""})
}

class FormCollectionWeb(cik:String, date:String="", formType:String="13F") extends LazyLogging {
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("edgar-spray-client")
  import system.dispatcher // execution context for futures below
  private var forms:Seq[XmlFormCollection]=List()

  def Invoke(f:Seq[XmlFormCollection]=>Unit) {
    logger.debug("Calling into edgar for cik: "+cik)

    def responseFuture(start:Int) = {
      val pipeline = sendReceive ~> unmarshal[String]
      pipeline {
        val r = ReqConfig(cik, start, date, formType)
        logger.debug("GET: "+r.toString)
        Get(r toString)
      }
    }

    def futureOnComplete(start:Int):Unit =
      responseFuture(start) onComplete {

        case Success(s) =>
          logger.debug("onComplete success")
          try {
            val fm = XmlFormCollection(scala.xml.XML.loadString(s))
            val sz = fm.entries.size
            logger.debug("Returned forms: " + sz)
            forms = forms ++ List(fm)
            val start = (for (x <- forms) yield (x.entries.size)).foldLeft(0)(_ + _)
            if (sz > 0) {
              // query until it returns 0
              logger.debug("Launch futureOnComplete with start=" + start)
              futureOnComplete(start)
            } else {
              f(forms)
              shutdown()
            }
          }catch{
            case e:Exception =>
              logger.error("Exception: "+e)
              shutdown()
          }

        case Failure(error) =>
          logger.error("Couldn't get request. Error: "+error)
          shutdown()
      }

    futureOnComplete(0)
  }

  def shutdown(): Unit = {
    IO(Http).ask(Http.CloseAll)(1.second).await
    system.shutdown()
  }
}