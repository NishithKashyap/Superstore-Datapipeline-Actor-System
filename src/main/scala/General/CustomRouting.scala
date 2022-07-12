package General

import akka.routing.{NoRoutee, RoundRobinRoutingLogic, Routee, RoutingLogic, SmallestMailboxRoutingLogic}

import java.util.concurrent.atomic.AtomicLong
import scala.collection.immutable

class CustomRouting(number: Int) extends RoutingLogic {

 val next = new AtomicLong

 override def select(message: Any, routees: immutable.IndexedSeq[Routee]): Routee =
   if (routees.nonEmpty) {
     val size = routees.size
     val index = ((next.getAndIncrement / number) % size).asInstanceOf[Int]
     routees(if (index < 0) size + index else index)
   } else NoRoutee
}
