package Actors

import General.Utility._
import Streams.DataProcessingStream.DataProcessor
import Streams.ErrorHandlingStream._
import akka.actor.{ActorRef, FSM, Stash}
import akka.routing.RoundRobinRoutingLogic

import java.util.Date

object SlaveFSM {

 // a case class to hold all the purchase details
 case class PurchaseDetails(orderDate: Date, shipDate: Date, shipMode: String, customer: String,
                            segment: String, country: String, city: String, state: String,
                            region: String, category: String, subCategory: String, name: String,
                            sales: Float, quantity: Int, discount: Float, profit: Float)

 // header fields array
 val header = Array("Order Date","Ship Date","Ship Mode", "Customer", "Segment", "Country", "City",
   "State", "Region", "Category", "Sub-Category", "Name", "Sales", "Quantity", "Discount", "Profit")

 // specifying all the states required in the Slave fsm actor
 trait SlaveState
 case object Initial extends SlaveState
 case object Validation extends SlaveState

 RoundRobinRoutingLogic
 // specifying all the data classes required in the Slave fsm actor
 trait SlaveData
 case object Uninitialized extends SlaveData
 case class RowData(row: Array[String]) extends SlaveData

 // Message case classes
 case class InvalidData(row: Array[String], column: Int)
 case object Valid
 case class validateData()

 /**
   the slave actor that is going to validate data and send the data based on
   validation status to either data processing stream or error handling stream
  */
 class SlaveFSM extends FSM[SlaveState, SlaveData] with Stash {

   startWith(Initial, Uninitialized)

   /**
     The initial state accepts the row data from the master and changes the fsm
     state to validation with the data set to the RowData that it just received
     from the master.
    */
   when(Initial) {
     case Event(row: Array[String], Uninitialized) =>
       goto(Validation) using RowData(row)
   }

   /**
     In the validation state,
     - case Event(validateData(), RowData(row))
         the row data is sent to a function checkForValidity to check the validity
         of the fields in the row. The state remains unchanged here.

     - case Event(Valid, RowData(row))
         if the flow reaches this state, it indicates that the data is valid and
         and this valid row is sent to a function for further processing. All the
         messages in the queue are un-stashed and the state is re-initialized to
         initial

     - case Event(InvalidData(row, column), RowData(row1))
         if the flow reaches this state, it indicates that the data is invalid and
         and this invalid row is sent to a function for further processing. All the
         messages in the queue are un-stashed and the state is re-initialized to
         initial
    */
   when(Validation) {
     case Event(validateData(), RowData(row)) =>
       checkForValidity(row)
       stay()
     case Event(Valid, RowData(row)) =>
       addToCaseClass(row)
       unstashAll()
       goto(Initial) using Uninitialized
     case Event(InvalidData(row, column), RowData(row1)) =>
       invalidField(row, column)
       unstashAll()
       goto(Initial) using Uninitialized
     case Event(row: Array[String], RowData(r)) =>
       stash()
       stay()
   }

   /**
     This method is called on a valid row. It receives the valid row and converts
     the data as a case class object. This case class object is later sent to the
     data processing stream for further processing. The actorRef parameter can be
     used for testing purpose to make sure the case class object created is infact
     correct
    */
   def addToCaseClass(row: Array[String], actorRef: ActorRef = null) = {

     val format = new java.text.SimpleDateFormat("MM/dd/yyyy")
     val record = PurchaseDetails(format.parse(row(0)), format.parse(row(1)),
       row(2), row(3), row(4), row(5), row(6),
       row(7), row(8), row(9), row(10), row(11),
       row(12).toFloat, row(13).toInt,
       row(14).toFloat, row(15).toFloat)

     if(actorRef != null)
       actorRef ! record

     // send to normal data stream
     DataProcessor ! record
   }

   /**
     This method receives invalid row and sends it to the error handling stream
    */
   def invalidField(row: Array[String], invalidColumn: Int) = {

     ErrorHandler ! (row, header(invalidColumn))
   }

   /**
     This function takes in a row as input, checks the validity of the row elements
     and calls either the valid or invalid event of the validity state.
    */
   def checkForValidity(row: Array[String], actorRef: ActorRef = null) = {

     /**
       This function takes in the row header and the index as parameters and checks
       for validity of that particular element by using pattern matching techniques.
       It returns true if the field is valid, false otherwise.
      */
     def isValid(header: String, index: Int): Boolean = {

       header match {
         case "Order Date" | "Ship Date" if (!(row(index) matches dateRegEx)) => false
         case "Sales" | "Discount" | "Profit" if (!(row(index) matches floatRegEx)) => false
         case "Quantity" if (!(row(index) matches intRegEx)) => false
         case "Customer" | "Country" | "State" if((row(index).isEmpty)) => false
         case _ => true
       }
     }

     // have a field to hold the invalid record, if found
     var column: Int = 0
     // variable to keep an eye on the status of the validation
     var flag: Boolean = true
     // send each field of the row one-by-one to the isValid() function
     row.zipWithIndex.foreach(record => {
       if (flag) {
         flag = isValid(header(record._2), record._2)
         column = record._2
       }
     })

     // for testing purpose to check if the method is functioning as required
     if(actorRef != null){
       actorRef ! flag
     }
     else {
       // call either valid or invalid based on the flag (validity) status
       if (flag) {
         self ! Valid
       } else {
         self ! InvalidData(row, column)
       }
     }
   }
 }
}
