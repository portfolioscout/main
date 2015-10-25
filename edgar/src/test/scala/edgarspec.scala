package edgar
package test

import collection.mutable.Stack
import org.scalatest._
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.{Future, Await}
import scala.util.{Failure, Success}
import scala.concurrent.duration._

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
    val date = "20140101"
    logger.debug("first pull all forms")
    val resultAsync = Await.result(new FormWebCollector(cik).fetch(), 60.seconds)
    assert(resultAsync.isDefined)
    assert(resultAsync.get.entries.size>0)
    assert(resultAsync.get.cik == cik)
    assert(resultAsync.get.name.toLowerCase().matches(".*gotham.*"))
    val sz1= resultAsync.get.entries.size
    logger.debug("found forms: "+sz1)

    logger.debug("then pull forms since "+date)
    val resultAsync1 = Await.result(new FormWebCollector(cik, date).fetch(), 60.seconds)
    assert(resultAsync1.isDefined)
    assert(resultAsync1.get.entries.size>0)
    assert(resultAsync1.get.cik == cik)
    assert(resultAsync1.get.name.toLowerCase().matches(".*gotham.*"))
    val sz2= resultAsync1.get.entries.size
    logger.debug("found forms: "+sz2)
    assert(sz2<sz1)
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
