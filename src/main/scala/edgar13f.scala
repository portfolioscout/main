package edgar13f

case class Config(cik: String = "", verbose: Boolean = false,
                  mode: String = "")

object Edgar13F{
  def main (args: Array[String]){
    val parser = new scopt.OptionParser[Config]("scopt") {
      head("edgar13f", "0.1")
      opt[String]("cik") action { (x, c) =>
        c.copy(cik = x) } text("cik of a fund")
      opt[Unit]("verbose") action { (_, c) =>
        c.copy(verbose = true) } text("verbose output")
      help("help") text("prints this usage text")
      cmd("update") action { (_, c) =>
        c.copy(mode = "update") } text("obtain and update 13F forms.")
    }
    // parser.parse returns Option[C]
    parser.parse(args, Config()) match {
      case Some(config) =>
      // do stuff

      case None =>
      // arguments are bad, error message will have been displayed
    }
  }
}