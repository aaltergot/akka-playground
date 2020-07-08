package pg.circle

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import pg.setup.{Setup, SetupBehavior}

/** Represents interim node in double-linked chain */
object CircleNode {

  sealed trait Command
  case class SetPeers(left: ActorRef[Command], right: ActorRef[Command]) extends Command
  case class SendMessage(from: ActorRef[Command], message: String) extends Command
  private case class Propagate(sendMessage: SendMessage) extends Command

  case class SetupProps(
    nodeId: Int,
    cooldownRandomSeed: Long,
    cooldownMaxMillis: Long
  )

  case class SetupSuccessProps(nodeId: Int)

  type SetupFailureProps = Nothing

  type SetupCircleNode = Setup[Command, SetupProps, SetupSuccessProps, SetupFailureProps]

  /** Initial state: awaiting Setup */
  def apply(): Behavior[SetupCircleNode] =
    Behaviors.setup(ctx => SetupBehavior.decorate(setup(ctx)))

  case class Props(
    nodeId: Int,
    cooldown: Cooldown,
    timers: TimerScheduler[Command],
    availablePeers: Seq[ActorRef[Command]]
  )

  /**
   * Active state:
   * - Receive Hello
   * - Wait from 1 to cooldownMaxMillis millis
   * - Propagate Hello to available peer
   * Peer becomes unavailable if it sends Hello to current.
   */
  private def active(data: Props): Behavior[Command] = Behaviors.receivePartial[Command] {

    case (_, SetPeers(left, right)) =>
      data.timers.cancelAll()
      active(data.copy(availablePeers = Seq(left, right)))

    case (_, msg @ SendMessage(from, _)) =>
      val cd = data.cooldown.next()
      data.timers.startSingleTimer(Propagate(msg), cd)
      active(data.copy(availablePeers = data.availablePeers.filter(_ != from)))

    case (ctx, Propagate(msg)) =>
      if (data.availablePeers.isEmpty) {
        ctx.log.info("{}", data.nodeId)
        data.timers.cancelAll()
        Behaviors.stopped
      } else {
        for (peer <- data.availablePeers) {
          peer ! msg.copy(from = ctx.self)
        }
        Behaviors.same
      }
  }

  /** Setup routine. See [[SetupBehavior]]. */
  private def setup
    (ctx: ActorContext[SetupCircleNode])
      (props: SetupProps)
  : Either[SetupFailureProps, (ActorRef[Command], SetupSuccessProps)] = {

    def runActive(): Behavior[Command] = Behaviors.withTimers[Command] { timers =>
      val p = Props(
        nodeId = props.nodeId,
        cooldown = Cooldown(props.cooldownMaxMillis, props.cooldownRandomSeed),
        timers = timers,
        availablePeers = Seq.empty
      )
      active(p)
    }

    val initializedRef: ActorRef[Command] =
      ctx.spawn(runActive(), s"circle-node-${props.nodeId}")

    Right((initializedRef, SetupSuccessProps(props.nodeId)))
  }
}
