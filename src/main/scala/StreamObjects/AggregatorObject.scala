package StreamObjects

import Actors.SlaveFSM.PurchaseDetails
import General.Utility._
import java.io.FileWriter

object AggregatorObject {

 /**
   This case class is used to find the aggregate of the separate fields, the add sum method
   returns a new case class object with the sum of previous and the new object fields
  */
 case class SumAggregator(sales: Float, quantity: Int, discount: Float, profit: Float) {

   def addSum(sales: Float, quantity: Int, discount: Float, profit: Float) = {

     SumAggregator(this.sales + sales, this.quantity + quantity,
       this.discount + discount, this.profit + profit)
   }
 }

 // case class objects to find the aggregated results
 var sumAggregator = SumAggregator(0.0f, 0, 0.0f, 0.0f)
 var bulkAggregator = SumAggregator(0.0f, 0, 0.0f, 0.0f)

 var counter: Int = 0

 /**
   This method takes in the PurchaseDetail case class type object and computes the aggregated
   sum by calling the addSum method. Once the computation is done, the fields of the
   sumAggregator case class object along with the averages of those fields are added to a file
  */
 def bulkSumAggregator(purchaseDetails: PurchaseDetails) = {

   val bulkFileWriter = new FileWriter(bulkProductInsightSinkFile)
   val bulkHeader = List("Total Sales", "Total Quantity", "Total Discount", "Total Profit",
     "Average Sales", "Average Quantity", "Average Discount", "Average Profit")
   bulkFileWriter.write(bulkHeader.toString.stripPrefix("List(").stripSuffix(")") + "\n")

   counter += 1

   bulkAggregator = bulkAggregator.addSum(purchaseDetails.sales, purchaseDetails.quantity,
     purchaseDetails.discount, purchaseDetails.profit)

   bulkFileWriter.write(bulkAggregator.toString.stripPrefix("SumAggregator(").stripSuffix(")")
     + "," + (bulkAggregator.sales / counter).toString
     + "," + (bulkAggregator.quantity / counter).toString
     + "," + (bulkAggregator.discount / counter).toString
     + "," + (bulkAggregator.profit / counter).toString)

   bulkFileWriter.close()
 }

 /**
   This method takes in the PurchaseDetail case class type object and computes the aggregated
   sum by calling the addSum method. Once the computation is done, the fields of the
   sumAggregator case class object are added to a file.
  */
 def calculateSum(purchaseDetails: PurchaseDetails) = {

   val sumFileWriter = new FileWriter("target/files/" + categoryFilter + "_2017.csv")
   val sumHeader = List("Sales", "Quantity", "Discount", "Profit")
   sumFileWriter.write(sumHeader.toString.stripPrefix("List(").stripSuffix(")") + "\n")

   sumAggregator = sumAggregator.addSum(purchaseDetails.sales, purchaseDetails.quantity,
     purchaseDetails.discount, purchaseDetails.profit)

   sumFileWriter.write(sumAggregator.toString.stripPrefix("SumAggregator(").stripSuffix(")"))

   sumFileWriter.close()
 }
}
