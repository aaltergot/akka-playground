package pg.chaintravel

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import ChainTravel._

import scala.collection.mutable

class ChainTravel(
  ctx: ActorContext[Command]
) extends AbstractBehavior[Command](ctx) {

  private var n: Int = -1
  private val nodesMap = mutable.Map[Int, ActorRef[Visit]]()

  override def onMessage(msg: Command): Behavior[Command] = msg match {

    case Travel(n) =>
      if (this.n > 0 || n < 1) {  // illegal
        this
      } else {
        this.n = n
        val terminal = ctx.spawn(ChainTerminal(), s"terminal-$n")
        val tssAdapter = ctx.messageAdapter[ChainTerminal.SetupSuccess] { tss =>
          NodeSetupSuccess(tss.nodeId, tss.visitRef)
        }
        terminal ! ChainTerminal.Setup(ChainTerminal.Settings(n), tssAdapter)
        ctx.watchWith(terminal, Terminated)  // watch terminal termination
        this
      }

    case NodeSetupSuccess(nodeId, visitRef) =>
      if (n < 0) {  // illegal (haven't seen "n" yet)
        this
      } else if (nodeId == 1) {  // all set up
        visitRef ! Visit(0, Seq.empty)
        this
      } else {
        nodesMap(nodeId) = visitRef
        val linkedNodeId = nodeId - 1
        if (nodeId > 0 && !nodesMap.contains(linkedNodeId)) {
          val node = ctx.spawn(ChainNode(), s"node-$linkedNodeId")
          val nssAdapter = ctx.messageAdapter[ChainNode.SetupSuccess] { css =>
            NodeSetupSuccess(css.nodeId, css.visitRef)
          }
          node ! ChainNode.Setup(ChainNode.Settings(linkedNodeId, visitRef), nssAdapter)
        }
        this
      }

    case Terminated =>
      Behaviors.stopped
  }
}

object ChainTravel {

  trait Command
  case class Travel(n: Int) extends Command
  case class NodeSetupSuccess(nodeId: Int, visitRef: ActorRef[Visit]) extends Command
  case object Terminated extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup(ctx => new ChainTravel(ctx))
}
