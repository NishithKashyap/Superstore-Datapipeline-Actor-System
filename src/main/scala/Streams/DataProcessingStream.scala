package Streams

import Actors.SlaveFSM.PurchaseDetails
import StreamObjects.FlowObject._
import StreamObjects.SinkObject.{bulkQuantitySum, categoryYearFilterSum, writeToCsvSink}
import General.Utility._

import akka.stream.scaladsl.{Broadcast, GraphDSL, Sink, Source}
import akka.stream.{OverflowStrategy, SinkShape}

object DataProcessingStream {

 // initializing an actor source with buffer size 15 and drop head as the overflow strategy
 val actorPoweredSource = Source.actorRef[PurchaseDetails](bufferSize = 15, overflowStrategy = OverflowStrategy.dropHead)

 /**
   This Sink GraphDSL returns a graph shape. It takes the input and sends it into the
   categoryFlow (flow which filters based on category) shape. The output from this flow
   is broadcasted (fanOut) into 2 seperate paths. One to the writeToCsv sink and the
   other to the yearFlow (flow which filters based on year), which sends the filtered
   data to the categoryYearFilterSum sink.
  */
 val categoryYearFilterGraph = GraphDSL.create() { implicit builder =>
   import GraphDSL.Implicits._

   val categoryFlow = builder.add(commonFlow("category"))
   val yearFlow = builder.add(commonFlow("year"))

   val fanOut = builder.add(Broadcast[PurchaseDetails](2))

   categoryFlow ~> fanOut.in
   fanOut.out(0) ~> writeToCsvSink
   fanOut.out(1) ~> yearFlow ~> categoryYearFilterSum

   SinkShape(categoryFlow.in)
 }

 /**
   This sink graph initially broadcasts its input, one to the categoryYearFilterGraph
   (the above sink graph) and the other to the quantityFilter (flow which filters based
    on quantity). This later flow result is sent to the yearFilter, from where it goes
    to the bulkQuantitySum sink.
  */
 val sinkGraph = Sink.fromGraph(
   GraphDSL.create() { implicit builder =>
     import GraphDSL.Implicits._

     val quantityFilter = builder.add(commonFlow("quantity"))
     val yearFlow = builder.add(commonFlow("year"))

     val broadcast = builder.add(Broadcast[PurchaseDetails](2))

     broadcast.out(0) ~> categoryYearFilterGraph
     broadcast.out(1) ~> quantityFilter ~> yearFlow ~> bulkQuantitySum

     SinkShape(broadcast.in)
   }
 )

 val DataProcessor = actorPoweredSource.to(sinkGraph).async.run()

}
