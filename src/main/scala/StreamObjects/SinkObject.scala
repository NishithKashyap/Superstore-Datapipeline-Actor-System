package StreamObjects

import Actors.SlaveFSM.PurchaseDetails
import General.Utility.{csvFile}
import StreamObjects.AggregatorObject.{bulkSumAggregator, calculateSum}
import akka.Done
import akka.stream.scaladsl.Sink

import java.io.{FileWriter, PrintWriter}
import scala.concurrent.Future

object SinkObject {

 val fileWriter = new PrintWriter(new FileWriter(csvFile))
 var status = false

 /**
   A sink function to write the PurchaseDetails record into a csv file.
   Initially headers are added to the file. The sink then takes in the records
   one-by-one and writes these records in successive rows.
  */
 def writeToCsvSink: Sink[PurchaseDetails, Future[Done]] = {

   val headers = List("Order Date", "Ship Date", "Ship Mode", "Customer", "Segment", "Country", "City", "State",
     "Region", "Category", "Sub-Category", "Name", "Sales", "Quantity", "Discount", "Profit")
   fileWriter.write(headers.toString.stripPrefix("List(").stripSuffix(")") + "\n")

   Sink.foreach[PurchaseDetails](records => fileWriter.write(
     records.toString.stripPrefix("PurchaseDetails(").stripSuffix(")") + "\n"))

 }

 /**
  This function calls the bulkSumAggregator method on every object sent to the sink
  */
 def bulkQuantitySum = {

   Sink.foreach[PurchaseDetails](bulkSumAggregator)
 }

 /**
  This function calls the calculateSum method on every object sent to the sink
  */
 def categoryYearFilterSum = {

   Sink.foreach[PurchaseDetails](calculateSum)
 }
}
