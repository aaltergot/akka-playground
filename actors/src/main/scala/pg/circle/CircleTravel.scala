package pg.circle

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import CircleTravel._
import pg.setup.Setup

import scala.collection.mutable
import scala.util.Random

class CircleTravel(
  ctx: ActorContext[Command]
) extends AbstractBehavior[Command](ctx) {

  private var n: Int = -1
  private val nodesMap = mutable.Map[Int, ActorRef[CircleNode.Command]]()

  override def onMessage(msg: Command): Behavior[Command] = msg match {

    case Travel(n) if n > 0 =>
      this.n = n

      val nodeSetupResultAdapter = ctx.messageAdapter[
        Either[
          CircleNode.SetupFailureProps,
          (ActorRef[CircleNode.Command], CircleNode.SetupSuccessProps)
        ]
      ] {
        case Left(_) => DoNothing
        case Right((node, props)) => NodeInitialized(props.nodeId, node)
      }

      for (i <- 1 to n) {
        val node = ctx.spawn(CircleNode(), s"node-$i")
        node ! Setup[
          CircleNode.Command,
          CircleNode.SetupProps,
          CircleNode.SetupSuccessProps,
          CircleNode.SetupFailureProps
        ](
          props = CircleNode.SetupProps(i, i, 100),
          replyTo = nodeSetupResultAdapter
        )
      }
      this

    case NodeInitialized(nodeId, ref) =>
      nodesMap(nodeId) = ref
      ctx.watchWith(ref, Terminated)
      if (nodesMap.size == n) {
        val random = new Random()
        val first = random.nextInt(n) + 1
        val mockedSender = ctx.messageAdapter[CircleNode.Command](_ => DoNothing)
        nodesMap(first) ! CircleNode.SendMessage(mockedSender, s"hello from: $first")
      }
      this

    case DoNothing =>
      this

    case Terminated =>
      Behaviors.stopped
  }
}


object CircleTravel {

  trait Command
  case class Travel(n: Int) extends Command
  case class NodeInitialized(nodeId: Int, ref: ActorRef[CircleNode.Command]) extends Command
  case object DoNothing extends Command
  case object Terminated extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup(ctx => new CircleTravel(ctx))
}
