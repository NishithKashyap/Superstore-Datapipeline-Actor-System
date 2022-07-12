package Actors

import Actors.MasterActor.Start
import akka.actor.typed.scaladsl.{Behaviors, LoggerOps, Routers}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop, SupervisorStrategy}
import akka.serialization.Serializer


object TypedActor {

 def main(args: Array[String]) = {
   val actor = ActorSystem(MasterActor(), "somename")
   actor ! Start
   actor ! Start
 }
}

object MasterActor {

 sealed trait Message
 final case object Start extends Message
 final case class Line(row: Array[String]) extends Message

 def apply(): Behavior[Message] = initialize()

 def initialize(): Behavior[Message] = Behaviors.setup { context =>
   Behaviors.receiveMessage[Message] {
     case Start =>
       val router = Routers.pool(10){
         Behaviors.supervise(Worker()).onFailure[Exception](SupervisorStrategy.stop)
       }
       val workerActor = context.spawn(router, "some")
       println(workerActor)
       workerActor ! Start
       Behaviors.stopped
   }.receiveSignal {
     case (_, PostStop) => println("bye")
       Behaviors.same

   }
 }

 def abc(): Behavior[Message] = {
   Behaviors.receive { (context, message) =>
     Behaviors.same
   }
 }
}

//object SlaveActor {
//
//  import MasterActor._
//
//  def apply(): Behavior[Message] = initialize()
//
//  def initialize(): Behavior[Message] = Behaviors.setup { context =>
//    Behaviors.receiveMessage {
//      case Start =>
//        val router = Routers.pool(10) {
//          Behaviors.supervise(Worker()).onFailure[Exception](SupervisorStrategy.restart)
//        }
//        val workerActor = context.spawn(router, "so")
//        println(workerActor)
//        Behaviors.same
//    }
//  }
//}
