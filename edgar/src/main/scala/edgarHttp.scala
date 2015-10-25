package edgar

import scala.concurrent.{Promise, Future}
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
                      dateBefore:String="",
                      ftype:String="",
                      owner:String="include", // TODO apparently bug n sec API. Owner=exclude is ignored.
                      count:Int=10,
                      output:String="atom",
                      url:String="http://www.sec.gov/cgi-bin/browse-edgar?action=getcompany"){
  override def toString:String ={
    url+
    "&CIK="+cik+
    "&type="+ftype+
    "&dateb="+dateBefore+
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

case class XmlFormCollection(xml: Elem, entries: Seq[Node]){
  val feed = xml \\ "feed"
  val author = feed \ "author"
  val companyInfo = feed \ "company-info"
  val name = companyInfo \ "conformed-name" text
  val cik = companyInfo \ "cik" text
  override def toString:String = "name:\""+name+
    "\"\tcik:"+cik+
    "\tentries:"+entries.toList.size+
    "\tlast:"+(if(!entries.isEmpty){XmlForm(entries.last)}else{""})
}

class FormWebCollector(cik:String, dateBefore:String="", formType:String="13F") extends LazyLogging {
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("edgar-spray-client")
  import system.dispatcher // execution context for futures below
  var forms:Option[XmlFormCollection]=None

  def fetch():Future[Option[XmlFormCollection]] ={
    logger.debug("Calling into edgar for cik: "+cik)
    val p = Promise[Option[XmlFormCollection]]()

    def responseFuture(start:Int) = {
      val pipeline = sendReceive ~> unmarshal[String]
      pipeline {
        val r = ReqConfig(cik, start, dateBefore, formType)
        logger.debug("GET: "+r.toString)
        Get(r toString)
      }
    }

    def loopTask(start:Int):Unit =
      responseFuture(start) onComplete {

        case Success(s) =>
          logger.debug("onComplete success")
          try {
            val xml = scala.xml.XML.loadString(s)
            val fm = XmlFormCollection(xml, xml \\ "feed" \ "entry")
            val sz = fm.entries.size

            if(forms.isEmpty) {
              forms = Some(fm)
            }else if (sz>0){
              val newEntries = forms.get.entries  ++ fm.entries
              forms= Some(forms.get.copy(entries=newEntries))
            }

            logger.debug("Returned forms: " + sz)
            if (sz > 0) {
              // query until it returns 0
              val start = forms.get.entries.size
              logger.debug("Launch loopTask with start=" + start)
              loopTask(start)
            } else {
              p.success(forms)
              shutdown()
            }
          }catch{
            case e:Exception =>
              logger.error("Exception: "+e)
              p.failure(e)
              shutdown()
          }

        case Failure(error) =>
          logger.error("Couldn't get request. Error: "+error)
          p.failure(error)
          shutdown()
      }

    Future{loopTask(0)}
    p.future
  }

  def shutdown(): Unit = {
    IO(Http).ask(Http.CloseAll)(1.second).await
    system.shutdown()
  }
}