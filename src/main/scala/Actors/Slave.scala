package Actors

import General.PreProcessing.header
import Streams.DataProcessingStream.DataProcessor
import Streams.ErrorHandlingStream._
import akka.actor.{Actor, ActorRef}

import java.util.Date

object Slave {

 // a case class to hold all the purchase details
 case class PurchaseDetails(orderDate: Date, shipDate: Date, shipMode: String, customer: String,
                            segment: String, country: String, city: String, state: String,
                            region: String, category: String, subCategory: String, name: String,
                            sales: Float, quantity: Int, discount: Float, profit: Float)

 case class Valid(row: Array[String])

 case class Invalid(row: Array[String], column: String)

 // slave actor class
 class Slave extends Actor {

   override def receive: Receive = initial

   def initial(): Receive = {
     case row: Array[String] =>
       validation(row, self)
     case Valid(row: Array[String]) => addToCaseClass(row)
     case Invalid(row: Array[String], column: String) => invalidField(row, column)
   }

   def validation(row: Array[String], actorRef: ActorRef) = {

     val dateRegEx = "([1-9]|1[0-2])/([1-9]|1[0-9]|2[0-9]|3[01])/(19|20)[0-9]{2}"
     val floatRegEx = "([-]?)[0-9]+(.[0-9]*)?"
     val intRegEx = "[0-9]*"
     var flag: Boolean = true
     var faultColumn: Int = -1

     def isValid(header: String, index: Int): Boolean = {

       header match {
         case "Order Date" | "Ship Date" if (!(row(index) matches dateRegEx)) => false
         case "Sales" | "Discount" | "Profit" if (!(row(index) matches floatRegEx)) => false
         case "Quantity" if (!(row(index) matches intRegEx)) => false
         case _ => true
       }
     }

     var column: Int = 0
     row.zipWithIndex.foreach(record => {
       if (flag) {
         flag = isValid(header(record._2), record._2)
         column = record._2
       }
     })

     if (flag)
       actorRef ! Valid(row)
     else
       actorRef ! Invalid(row, header(column))
   }

   def addToCaseClass(row: Array[String]) = {

     val format = new java.text.SimpleDateFormat("MM/dd/yyyy")
     val record = PurchaseDetails(format.parse(row(0)), format.parse(row(1)),
       row(2), row(3), row(4), row(5), row(6),
       row(7), row(8), row(9), row(10), row(11),
       row(12).toFloat, row(13).toInt,
       row(14).toFloat, row(15).toFloat)

     DataProcessor ! record
   }

   def invalidField(row: Array[String], column: String) = {

     // send to error handling stream
     ErrorHandler ! (row, column)
   }
 }

}
