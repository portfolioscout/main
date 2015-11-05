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
import java.time.LocalDate


case class ReqConfig(cik:String,
                      start:Int = 0,
                      dateFrom:String="",
                      dateTo:String="",
                      ftype:String="",
                      count:Int=10,
                      output:String="atom",
                      url:String="http://www.sec.gov/cgi-bin/browse-edgar?action=getcompany"){
  override def toString:String ={
    url+
    "&CIK="+cik+
    "&type="+ftype+
    "&start="+start+
    "&count="+count+
    "&output="+output
  }
}

case class XmlForm(entry:Node){
  val content = entry \ "content"
  val filingDate = content \ "filing-date" text
  override def toString =
  "filing date:"+filingDate
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

class FormWebCollector(cik:String, dateFrom:String="", dateTo:String="", formType:String="13F") extends LazyLogging {
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("edgar-spray-client")
  import system.dispatcher // execution context for futures below
  var forms:Option[XmlFormCollection]=None
  private var _start =0

  def fetch():Future[Option[XmlFormCollection]] ={
    logger.debug("Calling into edgar for cik: "+cik)
    val p = Promise[Option[XmlFormCollection]]()

    def responseFuture(start:Int) = {
      val pipeline = sendReceive ~> unmarshal[String]
      pipeline {
        val r = ReqConfig(cik, start, dateFrom, dateTo, formType)
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
            logger.debug("Returned forms: " + sz)

            _start +=  sz
            // filter forms based on date
            val ne = if(!dateFrom.isEmpty || !dateTo.isEmpty) {
              fm.entries.filter{
                x=>{
                  val y = XmlForm(x)
                  val t = LocalDate.parse(y.filingDate)
                  (if(!dateFrom.isEmpty) (t.isAfter(LocalDate.parse(dateFrom)) ||
                    t.isEqual(LocalDate.parse(dateFrom))
                  ) else true) &&
                    (if(!dateTo.isEmpty) (t.isBefore(LocalDate.parse(dateTo)) ||
                      t.isEqual(LocalDate.parse(dateTo))
                    ) else true)
                }
              }
            } else {
              fm.entries
            }
            val fm1 = fm.copy(entries = ne)
            val sz1 = fm1.entries.size
            logger.debug("Filtered forms: " + sz1)

            if(forms.isEmpty) {
              forms = Some(fm1)
            }else if (sz1>0){
              val newEntries = forms.get.entries  ++ fm1.entries
              forms= Some(forms.get.copy(entries=newEntries))
            }

            if (sz > 0) {
              // query until it returns 0
              logger.debug("Launch loopTask with start=" + _start)
              loopTask(_start)
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