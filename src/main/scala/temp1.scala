import java.util

object temp1 extends App {

  val t1 = System.nanoTime

  case class Payments(id: Int, amt: Float)
  def getList(payments: List[Payments]): util.List[Payments] ={
    val vP = new util.ArrayList[Payments]()
    for(p <- payments){
      if(p.amt < 0){
        val amt = p.amt;
        for (pT <- payments){
          if(p.id != pT.id){
            if(pT.amt > 0 && p.amt == pT.amt){
              if(!vP.contains(p))
                vP.add(p)
              if(!vP.contains(pT))
                vP.add(pT)
            }
          }
        }
      }
    }
    vP;
  }

  def modified(payments: List[Payments]): List[Payments] = {
    var vP = List[Payments]()
    payments.foreach { p =>
      if (!vP.contains(p)) {
        payments.foreach { pT =>
          if (!vP.contains(pT)) {
            if (p.id != pT.id && (p.amt == -pT.amt)) {
              vP = vP:+p
              vP = vP:+pT
            }
          }
        }
      }
    }
    vP;
  }

  val p = List(Payments(1, 5), Payments(2, -3), Payments(3, 3), Payments(4, 2), Payments(5,-2), Payments(6, 2), Payments(7, -2))
  val vp = modified(p)
  print(vp)

  val duration = (System.nanoTime - t1) / 1e9d
  print(duration)
}
