package pg.chaintravel

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

/** Represents interim node in single-linked chain */
object ChainNode {

  sealed trait Command

  case class Setup(settings: Settings, replyTo: ActorRef[SetupSuccess]) extends Command
  case class SetupSuccess(nodeId: Int, visitRef: ActorRef[Visit])

  case class WrappedVisit(visit: Visit) extends Command

  /** A node must have an ID and send Visit message to next node */
  case class Settings(nodeId: Int, next: ActorRef[Visit])

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
   * - Increment numberOfHopsTravelled
   * - Populate hops with current HopInfo
   * - Send to next
   * - Keep same behavior
   */
  def active(settings: Settings): Behavior[Command] = Behaviors.receiveMessagePartial[Command] {

    // wrapped Visit handler
    case WrappedVisit(msg) =>
      val newNumberOfHops = msg.numberOfHopsTravelled + 1
      val hopInfo = HopInfo(settings.nodeId, System.currentTimeMillis())
      val newHops = msg.hops :+ hopInfo

      settings.next ! Visit(newNumberOfHops, newHops)

      Behaviors.same
  }
}
