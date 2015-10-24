package edgar
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

case class Config(cik: String = "", verbose: Boolean = false,
                  mode: Option[Config=>Unit] = None)

object Edgar{
  val logger = Logger(LoggerFactory.getLogger("name"))

  def main (args: Array[String]){

    def list(config: Config): Unit ={
      logger.debug("option: "+config.mode + " "+config.cik)
      new FormCollectionWeb(config.cik).Invoke({forms=>
        if(forms.isEmpty){
          logger.info("No forms fetched")
        } else {
          val sz = (for(f<-forms) yield(f.entries.size)).foldLeft(0){_ + _}
          logger.debug("Fetched: "+forms.head.toString +
          "\t Total forms: "+sz)
        }

      })
    }

    val parser = new scopt.OptionParser[Config]("scopt") {
      head("edgar13f", "0.1")
      opt[String]("cik") action { (x, c) =>
        c.copy(cik = x) } text("cik of a fund")
      opt[Unit]("verbose") action { (_, c) =>
        c.copy(verbose = true) } text("verbose output")
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