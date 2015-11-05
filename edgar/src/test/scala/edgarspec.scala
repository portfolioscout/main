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
    val dateFrom = "2014-01-01"
    logger.debug("first pull all forms since date")
    val resultAsync = Await.result(new FormWebCollector(cik, dateFrom=dateFrom).fetch(), 60.seconds)
    assert(resultAsync.isDefined)
    assert(resultAsync.get.entries.size>0)
    assert(resultAsync.get.cik == cik)
    assert(resultAsync.get.name.toLowerCase().matches(".*gotham.*"))
    val sz1= resultAsync.get.entries.size
    logger.debug("found forms: "+sz1)
    resultAsync.get.entries.toList.foreach{
      x=>
        {
          val t = LocalDate.parse(x \ "content" \ "filing-date" text)
          logger.debug(t.toString)
        }
    }
    logger.debug("node text: "+(resultAsync.get.entries.last \ "content" \ "filing-date"))
    val t1 = LocalDate.parse(resultAsync.get.entries.last \ "content" \ "filing-date" text)
    val t2 = LocalDate.parse(dateFrom)
    assert(t2.isBefore(t1) || t2 == t1)
  }

  "WebFormFetcher" should "should be able to pull only forms before 20140101 for a Gotham Capital with cik 0001510387" in {

    val cik = "0001510387"
    val dateTo = "2014-01-01"
    logger.debug("first pull all forms since date")
    val resultAsync = Await.result(new FormWebCollector(cik, dateTo=dateTo).fetch(), 60.seconds)
    assert(resultAsync.isDefined)
    assert(resultAsync.get.entries.size>0)
    assert(resultAsync.get.cik == cik)
    assert(resultAsync.get.name.toLowerCase().matches(".*gotham.*"))
    val sz1= resultAsync.get.entries.size
    logger.debug("found forms: "+sz1)
    resultAsync.get.entries.toList.foreach{
      x=>
      {
        val t = LocalDate.parse(x \ "content" \ "filing-date" text)
        logger.debug(t.toString)
      }
    }
    logger.debug("node text: "+(resultAsync.get.entries.last \ "content" \ "filing-date"))
    val t1 = LocalDate.parse(resultAsync.get.entries.last \ "content" \ "filing-date" text)
    val t2 = LocalDate.parse(dateTo)
    assert(t2.isAfter(t1) || t2 == t1)
  }

  "WebFormFetcher" should "should be able to pull only forms " +
    "before 2012-11-14 and after 2011-05-16 for a Gotham Capital with cik 0001510387" in {

    val cik = "0001510387"
    val dateTo = "2012-11-14"
    val dateFrom = "2011-05-16"
    logger.debug("pull all forms between "+dateFrom+" and "+dateTo)
    val resultAsync = Await.result(new FormWebCollector(cik, dateTo=dateTo, dateFrom=dateFrom).fetch(), 60.seconds)
    assert(resultAsync.isDefined)
    assert(resultAsync.get.entries.size>0)
    assert(resultAsync.get.cik == cik)
    assert(resultAsync.get.name.toLowerCase().matches(".*gotham.*"))
    val sz1= resultAsync.get.entries.size
    logger.debug("found forms: "+sz1)
    resultAsync.get.entries.toList.foreach{
      x=>
      {
        val t = LocalDate.parse(x \ "content" \ "filing-date" text)
        logger.debug(t.toString)
      }
    }
    logger.debug("node text: "+(resultAsync.get.entries.last \ "content" \ "filing-date"))
    val t1 = LocalDate.parse(resultAsync.get.entries.last \ "content" \ "filing-date" text)
    val t2 = LocalDate.parse(dateTo)
    assert(t2.isAfter(t1) || t2 == t1)
    val t3 = LocalDate.parse(dateFrom)
    assert(t3.isBefore(t1) || t3 == t1)
  }

  "WebFormFetcher" should "should not be able to pull any forms " +
    "before 2011-05-16  and after 2012-11-14 for a Gotham Capital with cik 0001510387" in {

    val cik = "0001510387"
    val dateTo = "2011-05-16"
    val dateFrom = "2012-11-14"
    logger.debug("pull all forms between "+dateFrom+" and "+dateTo)
    val resultAsync = Await.result(new FormWebCollector(cik, dateTo=dateTo, dateFrom=dateFrom).fetch(), 60.seconds)
    assert(resultAsync.isDefined)
    assert(resultAsync.get.entries.size==0)
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
