package Actors

import General.PreProcessing.header
import Streams.DataProcessingStream.DataProcessor
import Streams.ErrorHandlingStream._
import akka.actor.Actor

import java.util.Date

object SlaveContextBecome {

 // a case class to hold all the purchase details
 case class PurchaseDetails(orderDate: Date, shipDate: Date, shipMode: String, customer: String,
                            segment: String, country: String, city: String, state: String,
                            region: String, category: String, subCategory: String, name: String,
                            sales: Float, quantity: Int, discount: Float, profit: Float)

 val dateRegEx = "([1-9]|1[0-2])/([1-9]|1[0-9]|2[0-9]|3[01])/(19|20)[0-9]{2}"
 val floatRegEx = "([-]?)[0-9]+(.[0-9]*)?"
 val intRegEx = "[0-9]*"


 case class validateData()

 case object CheckNow

 case object Valid

 case object Invalid

 // slave actor class
 class SlaveCopy extends Actor {

   override def receive: Receive = initial

   def initial: Receive = {
     case row: Array[String] =>
       context.become(validation(row))
       self ! validateData()
   }

   def validation(row: Array[String]): Receive = {
     case validateData() =>
       checkForValidity(row)
     case "Valid" =>
       addToCaseClass(row)
       context.become(initial)
     case "Invalid" =>
       println("invalid")
       invalidField(row)
       context.become(initial)
   }

   def addToCaseClass(row: Array[String]) = {

     val format = new java.text.SimpleDateFormat("MM/dd/yyyy")
     val record = PurchaseDetails(format.parse(row(0)), format.parse(row(1)),
       row(2), row(3), row(4), row(5), row(6),
       row(7), row(8), row(9), row(10), row(11),
       row(12).toFloat, row(13).toInt,
       row(14).toFloat, row(15).toFloat)

     // send to normal data stream
     DataProcessor ! record
   }

   def invalidField(row: Array[String]) = {

     println("invalid field")
     ErrorHandler ! "Invalid"
   }

   def checkForValidity(row: Array[String]) = {
     def isValid(header: String, index: Int): Boolean = {

       header match {
         case "Order Date" | "Ship Date" if (!(row(index) matches dateRegEx)) => false
         case "Sales" | "Discount" | "Profit" if (!(row(index) matches floatRegEx)) => false
         case "Quantity" if (!(row(index) matches intRegEx)) => false
         case _ => true
       }
     }

     var column: Int = 0
     var flag: Boolean = true
     row.zipWithIndex.foreach(record => {
       if (flag) {
         flag = isValid(header(record._2), record._2)
         column = record._2
       }
     })
     if (flag) {
       self ! "Valid"
     } else {
       self ! "Invalid"
     }
   }
 }

}
