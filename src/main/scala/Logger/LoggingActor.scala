package Logger

import General.Utility.actorSystem
import akka.actor.{Actor, ActorLogging, Props}

object LoggingActor {

 case class INFO(message: String)
 case class WARNING(message: String)
 case class ERROR(message: String)
 case class DEBUG(message: String)

 val logger = actorSystem.actorOf(Props[ErrorLogger])

 class ErrorLogger extends Actor with ActorLogging {

   override def receive: Receive = {
     case INFO(message: String) => log.info(message)
     case WARNING(message: String) => log.warning(message)
     case ERROR(message: String) => log.error(message)
     case DEBUG(message: String) => log.debug(message)
     case _ =>
   }
 }

}
