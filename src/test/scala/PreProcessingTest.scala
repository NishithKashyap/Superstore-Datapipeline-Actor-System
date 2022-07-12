import Actors.Master._
import akka.actor.{ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestFSMRef, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}

import scala.concurrent.duration._
import Actors.SlaveFSM._
import General.Utility._
import StreamObjects.FlowObject._
import StreamObjects.SinkObject._
import akka.Done
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.testkit.scaladsl.{TestSource}
import com.typesafe.config.ConfigFactory

import akka.actor.testkit.typed.scaladsl.LoggingTestKit

import scala.concurrent.Future
import scala.util.{Failure, Success}

class PreProcessingTest extends TestKit(ActorSystem("actorSystem", ConfigFactory.load().getConfig("interceptingLogMessages")))
                       with ImplicitSender
                       with WordSpecLike
                       with BeforeAndAfterAll
                       with MustMatchers{

 override def afterAll(): Unit = {
   TestKit.shutdownActorSystem(system)
 }

 val validRecord: Array[String] = Array("11/3/2021", "11/8/2021", "Mode", "Customer", "Segment", "Country",
   "City", "State", "Region", "Category", "Sub-Category", "Name", "273.9", "5", "1.2", "43.7")

 val invalidRecord: Array[String] = Array("11/3/2021", "118/2021", "Mode", "Customer", "Segment",
   "Country", "City", "State", "Region", "Category", "Sub-Category", "Name", "273.9", "5", "1.2", "43.7")

 val purchaseDetails1 = PurchaseDetails(format.parse("11/3/2017"), format.parse("13/3/2017"),
   "Mode", "Customer", "Segment", "Country", "City", "State", "Region", "Category", "Sub-Category",
   "Name", 273.9f, 5, 1.2f, 43.7f)

 val purchaseDetails2 = PurchaseDetails(format.parse("11/9/2016"), format.parse("12/9/2016"),
   "Mode", "Customer", "Segment", "Country", "City", "State", "Region", "Furniture", "Sub-Category",
   "Name", 232.9f, 15, 21.2f, 33.7f)

 val master = system.actorOf(Props[Master])

 "Master" should {

   /**
    * Check if the initial counter variable value is set to 0
    */
   "Initial value of counter is 0" in {
     master ! GetCount
     expectMsg(0)
   }

   /**
    * For every row the master receives and passes it along to the
    * workers, the count is incremented by 1. This test case checks if the counter
    * variable works properly
    */
   "Count number of records parsed" in {
     val sender = TestProbe()
     for (i <- 1 to 5)
       sender.send(master, validRecord)
     sender.send(master, GetCount)
     val count = sender.expectMsgType[Int]
     count must equal(5)
   }

   /**
    * In the master class, if a illegal data is sent (data that is not of the
    * form Array[String] is considered illegal), the master throws a InvalidDataReceivedException.
    */
   "Check if error is thrown when you send wrong data" in {
     EventFilter[InvalidDataReceivedException](occurrences = 1) intercept {
       master ! "Definitely the wrong data"
     }
   }

   /**
    * If a valid data is sent to the master, the message is parsed without any exceptions
    * or error messages
    */
   "Send valid data" in {
     master ! validRecord
     expectNoMessage(1.second)
   }
 }

 "Slave " should {
   val fsm = TestFSMRef(new SlaveFSM, "FSM")

   /**
    * This test checks if the initial state and the data in the SlaveFSM actor class
    * is set correctly.
    * Initial state = Initial
    * Initial data = Uninitialized
    */
   "initially" in {
     fsm.stateName must equal(Initial)
     assert(fsm.stateData == Uninitialized)
   }

   /**
     1. Set the state of the actor to Validation and send a valid record.
     2. Call the validateData() event on the actor
     3. If the data is parsed without any error or exceptions, the state of the actor
        is set to Initial
     */
   "when valid data is passed" in {
     fsm.setState(stateName = Validation, stateData = RowData(validRecord))
     fsm ! validateData()
     assert(fsm.stateName == Initial) // valid data is successfully parsed
   }

   /**
     1. Set the state of the actor to Validation and send an invalid record.
     2. Call the validateData() event on the actor
     3. If the data is parsed without any error or exceptions, the state of the actor
        is set to Initial. A logger is invoked which logs an error message of the type
        INVALID FORMAT ERROR {invalid column name}
    */
   "when invalid data is passed" in {
     fsm.setState(stateName = Validation, stateData = RowData(invalidRecord))
     EventFilter.error(message = "INVALID FORMAT ERROR Ship Date") intercept {
       fsm ! validateData()
     }
     fsm.stateName must equal(Initial) // invalid data is logged without exception
   }

   /**
    * When a valid data is sent to the addToCaseClass() function in the slaveFSM actor,
    * it returns a data of type PurchaseDetails (case class)
   */
   "Send valid data to addToCaseClass function" in {
     val testProbe = TestProbe()
     fsm.underlyingActor.addToCaseClass(validRecord, testProbe.ref)
     testProbe.expectMsgType[PurchaseDetails]
   }

   /**
    * When data is sent to checkForValidity function in the SlaveFSM actor,
    * it returns a boolean value that indicates the status of the data. It returns true
    * for valid data and false for invalid data
   */
   "check for validity of valid data" in {
     val testProbe = TestProbe()
     fsm.underlyingActor.checkForValidity(validRecord, testProbe.ref)
     val flag = testProbe.expectMsgType[Boolean]
     flag must be(true)
   }

   "check for validity of invalid data" in {
     val testProbe = TestProbe()
     fsm.underlyingActor.checkForValidity(invalidRecord, testProbe.ref)
     val flag = testProbe.expectMsgType[Boolean]
     flag must be(false)
   }
 }

 "Data processing flows" should {

   /**
    * This test checks if the CategoryFilterFlow flow properly filters the items
    * based on the category mentioned in the conf folder. It is said to be working
    * if it returns only the purchaseDetails2 record and filters out purchaseDetails1
    */
   "Category Filter Flow should filter based on category" in {
     testFlow("category", purchaseDetails2)
   }

   /**
    * This test checks if the YearFilterFlow flow properly filters the items
    * based on the year as mentioned in the conf folder. It is said to be working
    * if it returns only the purchaseDetails1 record and filters out purchaseDetails2
    */
   "Year Filter Flow should filter based on year" in {
     testFlow("year", purchaseDetails1)
   }

   /**
     This test checks if the bulkQuantityFilter flow properly filters the items
     based on the quantity mentioned in the conf folder. It is said to be working
     if it returns only the purchaseDetails2 record and filters out purchaseDetails1.
    */
   "Bulk Quantity Filter" in {
     testFlow("quantity", purchaseDetails2)
   }

   /**
     The following function is designed to take in a flow function and a result of type
     PurchaseDetails as input and test the flow. Source is fed with 2 PurchaseDetails
     type field and it filters the records based on the criteria as mentioned in the
     flow function passed.
  */
   def testFlow(attribute: String, result: PurchaseDetails) = {
     val probe = TestProbe()
     val source = TestSource[PurchaseDetails]
       .via(commonFlow(attribute))
       .to(Sink.actorRef(probe.ref, onCompleteMessage = "completed", onFailureMessage = _ => "failed"))
       .run()

     source.sendNext(purchaseDetails1)
     source.sendNext(purchaseDetails2)
     probe.expectMsg(result)
   }
 }

 "Data processing sinks" should {
   import scala.concurrent.ExecutionContext.Implicits._

   /**
    * The writeToCsvSink takes in PurchaseDetails objects and writes the records
    * into a csv file. If the files are written successfully, a success message
    * is sent to the created Actor Probe.
    */
   "Write To CSV Sink should successfully write data to a csv file" in {
     testSink(writeToCsvSink)
   }

   /**
    * The bulkQuantitySum takes in PurchaseDetails objects, computes the total sum and
    * average values of quantity, discount, sales and profit. Then it writes these
    * records into a csv file. If the files are written successfully, a success
    * message is sent to the created Actor Probe.
    */
   "Bulk Quantity Sum sink computes the aggregation" in {
     testSink(bulkQuantitySum)
   }

   /**
    * The categoryYearFilterSum takes in PurchaseDetails objects, computes the total sum
    * quantity, discount, sales and profit. Then it writes these records into a csv file.
    * If the files are written successfully, a success message is sent to the created
    * Actor Probe.
    */
   "Category Year Filter Sum sink test" in {
     testSink(categoryYearFilterSum)
   }

   /**
    * This method takes in a sink function as parameter and tests the sink based on
    * its ability to successfully complete the job without any exceptions.
    */
   def testSink(function: Sink[PurchaseDetails, Future[Done]]): Unit = {
     val materialized = TestSource.probe[PurchaseDetails].toMat(function)(Keep.both).run()
     val (publisher, subscriber) = materialized
     val probe = TestProbe()

     publisher.sendNext(purchaseDetails1)
       .sendNext(purchaseDetails2)
       .sendComplete()

     subscriber.onComplete{
       case Success(_) => probe.ref ! "Success"
       case Failure(exception) => fail("Test case failed " + exception.printStackTrace)
     }
     probe.expectMsg("Success")
   }
 }
}