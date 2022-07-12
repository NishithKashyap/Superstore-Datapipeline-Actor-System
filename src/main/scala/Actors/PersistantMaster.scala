package Actors

import Actors.SlaveFSM.{SlaveFSM, validateData}
import General.CustomRouting
import General.Utility._
import akka.actor.{Props, Terminated}
import akka.persistence.PersistentActor
import akka.routing.{ActorRefRoutee, Router}

object PersistantMaster {

 class PersistantMaster extends PersistentActor {

   private val slaves = for (i <- 1 to number_of_workers) yield {
     val slave = context.actorOf(Props[SlaveFSM], s"slave_$i")
     context.watch(slave)
     ActorRefRoutee(slave)
   }

   var router = Router(new CustomRouting(2), slaves)

   var counter: Int = 0

   override def persistenceId: String = "persistent-master"

   override def receiveCommand: Receive = {

     case Terminated(ref) =>
       router.removeRoutee(ref)
       val newSlave = context.actorOf(Props[SlaveFSM])
       context.watch(newSlave)
       router.addRoutee(newSlave)

     case row: Array[String] =>
       router.route(row, self)
       router.route(validateData(), self)
   }

   override def receiveRecover: Receive = ???
 }
}
