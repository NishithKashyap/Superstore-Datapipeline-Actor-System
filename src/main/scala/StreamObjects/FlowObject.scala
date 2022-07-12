package StreamObjects

import Actors.SlaveFSM.PurchaseDetails
import General.Utility._
import akka.NotUsed
import akka.stream.scaladsl.Flow

object FlowObject {

 /**
  A common flow method that filters the records based on the attribute as supplied
  to it.
  */
 def commonFlow(attribute: String) = {
   Flow[PurchaseDetails].filter(record =>
     attribute match {
       case "category" => record.category == categoryFilter
       case "year" => record.orderDate.getYear == (financialYear - 1900)
       case "quantity" => record.quantity >= bulkQuantityValue
     }
   )
 }
}
