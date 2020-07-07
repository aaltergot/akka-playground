package pg.circle

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import CircleTravel._

import scala.collection.mutable
import scala.util.Random

class CircleTravel(
  ctx: ActorContext[Command]
) extends AbstractBehavior[Command](ctx) {

  override def onMessage(msg: Command): Behavior[Command] = msg match {

    case Travel(n) if n > 0 =>
      val random = new Random()

      val nodesMap = mutable.Map.empty[Int, ActorRef[CircleNode.Command]]
      for (i <- 1 to n) {
//        val node = ctx.spawn(CircleNode(), s"node-$i")
//        ctx.watchWith(node, Terminated)  // watch node termination
//        nodesMap(i) = node
      }

      nodesMap.foreachEntry { (i, node) =>
//        val left = if (i == 1) { nodesMap(n) } else { nodesMap(i - 1) }
//        val right = if (i == n) { nodesMap(1) } else { nodesMap(i + 1) }
//        node ! CircleNode.Setup(i, random.nextLong(), 100, left, right)
      }

      val first = random.nextInt(n) + 1
      val mockedSender = ctx.messageAdapter[CircleNode.Command](_ => DoNothing)
      nodesMap(first) ! CircleNode.SendMessage(mockedSender, s"hello from: $first")

      ctx.log.info("first is {}", first)

      this

    case Terminated =>
      Behaviors.stopped
  }
}


object CircleTravel {

  trait Command
  case class Travel(n: Int) extends Command
  case object DoNothing extends Command
  case object Terminated extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup(ctx => new CircleTravel(ctx))
}
