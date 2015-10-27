package edgar
package test

import collection.mutable.Stack
import org.scalatest._
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.{Future, Await}
import scala.util.{Failure, Success}
import scala.concurrent.duration._
import java.time.LocalDate

class EdgarSpec extends FlatSpec with Matchers{

  val logger = Logger(LoggerFactory.getLogger("name"))

  "WebFormFetcher" should "should be able to pull all forms for a Gotham Capital with cik 0001510387" in {

    val cik = "0001510387"
    logger.debug("cik: "+cik)
    val resultAsync = Await.result(new FormWebCollector(cik).fetch(), 60.seconds)
    assert(resultAsync.isDefined)
    assert(resultAsync.get.entries.size>0)
    assert(resultAsync.get.cik == cik)
    assert(resultAsync.get.name.toLowerCase().matches(".*gotham.*"))
  }

  "WebFormFetcher" should "should be able to pull only forms since 20140101 for a Gotham Capital with cik 0001510387" in {

    val cik = "0001510387"
    val date = "2014-01-01"
    logger.debug("first pull all forms since date")
    val resultAsync = Await.result(new FormWebCollector(cik, date).fetch(), 60.seconds)
    assert(resultAsync.isDefined)
    assert(resultAsync.get.entries.size>0)
    assert(resultAsync.get.cik == cik)
    assert(resultAsync.get.name.toLowerCase().matches(".*gotham.*"))
    val sz1= resultAsync.get.entries.size
    logger.debug("found forms: "+sz1)
    logger.debug("node text: "+(resultAsync.get.entries.last \ "content" \ "filing-date"))
    val t1 = LocalDate.parse(resultAsync.get.entries.last \ "content" \ "filing-date" text)
    var t2 = LocalDate.parse(date)
    assert(t2.isBefore(t1) || t2 == t1)
  }

  "WebFormFetcher" should "should be not pull any forms with invalid cik=0001xyz" in {

    val cik = "0001xyz"
    logger.debug("cik: "+cik)
    a [org.xml.sax.SAXParseException] should be thrownBy {
      val resultAsync = Await.result(new FormWebCollector(cik).fetch(), 60.seconds)
    }

  }


//  it should "throw NoSuchElementException if an empty stack is popped" in {
//    val emptyStack = new Stack[Int]
//    a [NoSuchElementException] should be thrownBy {
//      emptyStack.pop()
//    }
//  }
}
