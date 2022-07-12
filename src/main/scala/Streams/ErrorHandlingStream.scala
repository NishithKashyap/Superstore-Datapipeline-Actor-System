package Streams

import General.Utility._
import Logger.LoggingActor.ERROR
import akka.stream.scaladsl.{Broadcast, GraphDSL, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy, SinkShape}
import au.com.bytecode.opencsv.CSVWriter

import java.io.{BufferedWriter, FileWriter, PrintWriter}

object ErrorHandlingStream {

 implicit val materializer = ActorMaterializer()(actorSystem)

 val actorPoweredSource = Source.actorRef[(Array[String], String)](bufferSize = 15, overflowStrategy = OverflowStrategy.dropHead)

 def logError = {

   Sink.foreach[(Array[String], String)](message => logger ! ERROR(s"INVALID FORMAT ERROR ${message._2}"))
 }

 def sendMsgToFile = {

   Sink.foreach[(Array[String], String)](message => {
     val csvFile = new BufferedWriter(new FileWriter(errorRecordFileName + java.time.LocalDate.now + ".csv", true))
     val csvWriter = new CSVWriter(csvFile)
     csvWriter.writeNext(s"INVALID FORMAT ERROR ${message._2}")
     csvFile.close()
   })
 }

 def sendRecordToCSV = {

   Sink.foreach[(Array[String], String)](records => {
     val csvFile = new BufferedWriter(new FileWriter(errorLogFile + java.time.LocalDate.now + ".csv", true))
     val csvWriter = new CSVWriter(csvFile)
     csvWriter.writeNext(records._1.mkString(","))
     csvFile.close()
   })
 }

 val errorHandlingGraph = Sink.fromGraph(
   GraphDSL.create() { implicit builder =>
     import GraphDSL.Implicits._

     val broadcast = builder.add(Broadcast[(Array[String], String)](3))

     broadcast.out(0) ~> logError
     broadcast.out(1) ~> sendRecordToCSV
     broadcast.out(2) ~> sendMsgToFile

     SinkShape(broadcast.in)
   }
 )

 val ErrorHandler = actorPoweredSource.to(errorHandlingGraph).run()
}
