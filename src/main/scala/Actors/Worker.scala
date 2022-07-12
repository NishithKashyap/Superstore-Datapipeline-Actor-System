package Actors

import Actors.MasterActor.{Message, Start}
import akka.actor.typed.{Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.Future.never.value

object Worker {

 def apply(): Behavior[Message] = {
   Behaviors.receiveMessage[Message] {
     case Start =>
       println("hello")
       Behaviors.same
   }
 }


 final case class Zoo(primaryAttraction: Animal) extends MySerializable

 @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
 @JsonSubTypes(
   Array(
     new JsonSubTypes.Type(value = classOf[Lion], name = "lion"),
     new JsonSubTypes.Type(value = classOf[Elephant], name = "elephant")))

 sealed trait Animal
 final case class Lion(name: String) extends Animal
 final case class Elephant(name: String, age: Int) extends Animal
}



