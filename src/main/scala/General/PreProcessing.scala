package General

import General.Utility._
import Logger.LoggingActor.ERROR

import scala.io.Source

object PreProcessing extends App {

 // get data from the file
 val source = Source.fromFile(filename)

 // get the lines from source
 val lines = source.getLines()

 // get the headers and store it as an array
 val header = lines.next().split(",")

 /*
   get each line (record) and split the records based on "," and make sure that
   a particular entry with a comma in it is not split into two separate records.
   Once split, this is sent as a single record (Array[String]) to the master actor
  */
 try {
   while (lines.hasNext) {
     val row = lines.next().split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")
     master ! row
   }
 } catch {
   case exception: Exception => logger ! ERROR(s"Could not read the data ${exception}")
 }
 finally source.close()
}