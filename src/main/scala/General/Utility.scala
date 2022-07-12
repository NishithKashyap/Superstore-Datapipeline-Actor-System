package General

//import Actors.Master.Master
//import Logger.LoggingActor.ErrorLogger
import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

object Utility {

  // create a actor system variable
  val actorSystem = ActorSystem("MasterChild")

  // create a logging actor
  val logger = actorSystem.actorOf(Props[ErrorLogger], "loggingActor")

  // file name of the data source
  val filename = ConfigFactory.load("application.conf").getString("FILE_NAME")

  // initialize the master actor
//  val master = actorSystem.actorOf(Props[Master], "master")

  // number of slaves/workers
  val number_of_workers = ConfigFactory.load("application.conf").getInt("NO_OF_WORKERS")

  // Regex values for validation
  val format = new java.text.SimpleDateFormat("MM/dd/yyyy")
  val dateRegEx = "([1-9]|1[0-2])/([1-9]|1[0-9]|2[0-9]|3[01])/(19|20)[0-9]{2}"
  val floatRegEx = "([-]?)[0-9]+(.[0-9]*)?"
  val intRegEx = "[0-9]*"

  // Materializer
  implicit val materializer = ActorMaterializer()(actorSystem)

  final case class InvalidDataReceivedException(private val message: String = "Not the valid data") extends Exception
  case object GetCount

  // values declared inside config file
  val categoryFilter = ConfigFactory.load("application.conf").getString("categoryFilter")
  val financialYear = ConfigFactory.load("application.conf").getInt("FinancialYear")
  val csvFile = ConfigFactory.load("application.conf").getString("categorySinkFile")
  val bulkQuantityValue = ConfigFactory.load("application.conf").getInt("BulkQuantityValue")
  val bulkProductInsightSinkFile = ConfigFactory.load("application.conf").getString("BulkProductInsightSinkFile")
  val errorLogFile = ConfigFactory.load("application.conf").getString("ErrorLogFile")
  val errorRecordFileName = ConfigFactory.load("application.conf").getString("ErrorRecordFileName")
}
