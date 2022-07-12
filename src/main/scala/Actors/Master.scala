package Actors

import Actors.SlaveFSM._
import General.CustomRouting
import General.Utility._
import Logger.LoggingActor.INFO
import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, OneForOneStrategy, Props, ReceiveTimeout, SupervisorStrategy, Terminated}
import akka.routing.{ActorRefRoutee, Router}
import org.scalatest.time.Seconds

import scala.concurrent.duration._

object Master {

 /**
  * The main master actor that receives row records and distributes it to
  * the slaves based on the routing mechanism.
  */
 class Master extends Actor {

   // counter to keep a count of no. of records read
   var counter: Int = 0

   // timeout window to indicate that there are no more records to receive
   context.setReceiveTimeout(3 Seconds)

   // create the required amount of slaves (here, 10)
   private val slaves = for (i <- 1 to number_of_workers) yield {
     val slave = context.actorOf(Props[SlaveFSM], s"slave_$i")
     context.watch(slave)
     ActorRefRoutee(slave)
   }

   // configure the router with routing logic and the actors in them
   var router = Router(new CustomRouting(2), slaves)

   // supervisor strategy to take care of any setbacks that might occur
   override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
     case _ => Restart
   }

   override def receive: Receive = {
     // in case the slave actor terminates
     case Terminated(ref) =>
       router.removeRoutee(ref)
       val newSlave = context.actorOf(Props[SlaveFSM])
       context.watch(newSlave)
       router.addRoutee(newSlave)

     /**
       in case the master receives a row of type Array[String]
       the router routes this row data to the slave based on its routing strategy
       counter is incremented by one indicating a row is received and forwarded
      */
     case row: Array[String] =>
       router.route(row, self)
       router.route(validateData(), self)
       counter += 1

     // to get the present count of number of rows received and forwarded
     case GetCount =>
       logger ! INFO("yes")
       sender ! counter

     /**
       when timeout occurs, the total number of rows parsed is logged
       and the timeout window is set to undefined
      */
     case ReceiveTimeout =>
       logger ! INFO(s"Total number of records parsed $counter")
       context.setReceiveTimeout(Duration.Undefined)

     // in any other case, a InvalidDataReceivedException is thrown
     case _ =>
         throw InvalidDataReceivedException()
   }
 }
}