package pg.chaintravel

case class Visit(numberOfHopsTravelled: Int, hops: Seq[HopInfo])

case class HopInfo(nodeId: Int, timestamp: Long)

