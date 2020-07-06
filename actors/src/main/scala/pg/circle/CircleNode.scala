package pg.circle

import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration._
import scala.util.Random

/** Represents interim node in double-linked chain */
object CircleNode {

  sealed trait Command

  case class Setup(
    nodeId: Int,
    randomSeed: Long,
    left: ActorRef[Command],
    right: ActorRef[Command]
  ) extends Command

  case class Hello(sender: ActorRef[Command], msg: String) extends Command

  case class Propagate(msg: String) extends Command

  case class Data(
    nodeId: Int,
    random: Random,
    timer: TimerScheduler[Command],
    availablePeers: Set[ActorRef[Command]]
  )

  /** Initial state: awaiting Setup */
  def apply(): Behavior[Command] = Behaviors.withTimers { timer =>
    Behaviors.receiveMessagePartial[Command] {

      // Setup handler
      case Setup(nodeId, randomSeed, left, right) =>
        active(Data(nodeId, new Random(randomSeed), timer, Set(left, right)))
    }
  }

  /**
   * Active state:
   * - Receive Hello
   * - Wait from 1 to 100 millis
   * - Propagate Hello to available peer
   * Peer becomes unavailable if it sends Hello to current.
   */
  def active(data: Data): Behavior[Command] = Behaviors.receivePartial[Command] {

    case (_, Hello(sender, msg)) =>
      val backoff = (data.random.nextInt(100) + 1).millis
      data.timer.startSingleTimer(Propagate(msg), backoff)
      active(data.copy(availablePeers = data.availablePeers.filter(_ != sender)))

    case (ctx, Propagate(msg)) =>
      if (data.availablePeers.isEmpty) {
        ctx.log.info("{}", data.nodeId)
        data.timer.cancelAll()
        Behaviors.stopped
      } else {
        for (peer <- data.availablePeers) {
          peer ! Hello(ctx.self, msg)
        }
        Behaviors.same
      }
  }
}
