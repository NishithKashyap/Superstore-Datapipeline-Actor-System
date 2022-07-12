import General.Utility.actorSystem
import akka.actor.TypedActor.dispatcher
import akka.pattern.after

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object temp extends App{

  class InvalidException() extends Exception(){}

  case class EPCPayments(id: Int)
  case class Customers(id: Int)

  def returnList(): Future[List[EPCPayments]] = Future {
    List(new EPCPayments(1), new EPCPayments(2))
  }

  def toFP(customers: Customers, attempts: Int, delay: FiniteDuration)(
  implicit ec: ExecutionContext): Future[EPCPayments] = {
    returnList()
      .flatMap { x =>
        if (x.isEmpty) {
          throw new InvalidException()
        }
        else {
          Future(x.head)(ec)
        }
      }(ec)
      .recover(
        case e: InvalidException => toF(customers, attempts - 1, delay)
      )
  }

  def toF(customers: Customers, attempts: Int, delay: FiniteDuration): Future[EPCPayments] = {
      Future {EPCPayments(1)}
  }
}

