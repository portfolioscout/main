package edgar
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import scala.util.{Success, Failure}


case class Config(cik: String = "",
                  verbose: Boolean = false,
                  dateFrom:String = "",
                  dateTo:String = "",
                  mode: Option[Config=>Unit] = None)

object Edgar{
  val logger = Logger(LoggerFactory.getLogger("name"))

  def main (args: Array[String]){

    def list(config: Config): Unit ={
      import scala.concurrent.ExecutionContext.Implicits.global
      logger.debug("option: "+config.mode + " "+config.cik)
      new FormWebCollector(config.cik,dateFrom=config.dateFrom, dateTo=config.dateTo).fetch().onComplete({
        case(Success(forms)) =>
          if(forms.isEmpty){
            logger.info("No forms fetched")
          } else {
            val sz = forms.get.entries.size
            logger.debug("Fetched: "+forms.head.toString +
              "\t Total forms: "+sz)
          }
        case Failure(err) =>
          logger.error("Error fetching forms: "+err)
      })
    }

    val parser = new scopt.OptionParser[Config]("scopt") {
      head("edgar13f", "0.1")
      opt[String]("cik") action { (x, c) =>
        c.copy(cik = x) } text("cik of a fund")
      opt[String]("date-from") action { (x, c) =>
        c.copy(dateFrom = x) } text("pull forms since date in the form YYYYMMDD")
      opt[String]("date-to") action { (x, c) =>
        c.copy(dateTo = x) } text("pull forms till date in the form YYYYMMDD")
      help("help") text("prints this usage text")
      cmd("list") action { (_, c) =>
        c.copy(mode = Some(list)) } text("list 13F forms.")
    }
    // parser.parse returns Option[C]
    parser.parse(args, Config()) match {
      case Some(config) =>
        if(config.mode.isEmpty) {
          logger.error("Invalid mode")
          parser.usage
        } else {
          (config.mode.get)(config)
        }


      case None =>
      // arguments are bad, error message will have been displayed
    }
    logger.info("Finished")
  }
}