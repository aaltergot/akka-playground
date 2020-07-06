package pg.chaintravel

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.LoggerOps

/** Represents terminal node that prints the result */
object ChainTerminal {

  sealed trait Command

  case class Setup(settings: Settings, replyTo: ActorRef[SetupSuccess]) extends Command
  case class SetupSuccess(nodeId: Int, visitRef: ActorRef[Visit])

  case class WrappedVisit(visit: Visit) extends Command

  /** A node must have an ID */
  case class Settings(nodeId: Int)

  /** Initial state: awaiting Settings */
  def apply(): Behavior[Command] = Behaviors.receivePartial[Command] {

    // Setup handler
    case (ctx, Setup(settings, replyTo)) =>
      val visitRef = ctx.messageAdapter(WrappedVisit)
      replyTo ! SetupSuccess(settings.nodeId, visitRef)
      active(settings)
  }

  /**
   * Active state:
   * - Populate hops with current HopInfo
   * - Print results
   * - Keep same behavior
   */
    def active(settings: Settings): Behavior[Command] = Behaviors.receivePartial[Command] {

      // wrapped Visit handler
      case (ctx, WrappedVisit(msg)) =>
        val hopInfo = HopInfo(settings.nodeId, System.currentTimeMillis())
        val hops = msg.hops :+ hopInfo

        // print stuff
        ctx.log.info("{}", msg.numberOfHopsTravelled)
        for (hop <- hops) {
          ctx.log.info2(
            "actor {}, message received {}",
            hop.nodeId,
            hop.timestamp
          )
        }

        Behaviors.stopped
    }
}
