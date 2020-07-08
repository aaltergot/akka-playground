package pg.circle

import akka.actor.testkit.typed.scaladsl.{LogCapturing, LoggingTestKit, ManualTime, ScalaTestWithActorTestKit}
import akka.actor.typed.ActorRef
import org.scalatest.wordspec.AnyWordSpecLike
import pg.setup.Setup

import scala.concurrent.duration._


class CircleNodeSpec
    extends ScalaTestWithActorTestKit(ManualTime.config)
    with AnyWordSpecLike
    with LogCapturing {

  import CircleNode._

  val manualTime: ManualTime = ManualTime()

  "CircleNode actor" must {

    "reply on Setup" in {
      val node = spawn(CircleNode())

      val unhandledProbe = createUnhandledMessageProbe()
      val leftProbe = createTestProbe[Command]()
      val rightProbe = createTestProbe[Command]()
      val setupReplyProbe = createTestProbe[
        Either[SetupFailureProps, (ActorRef[Command], SetupSuccessProps)]
      ]()

      val nodeId = 1
      val cooldownRandomSeed = 1L
      val cooldownMaxMillis = 100L
      val expectedCooldown = Cooldown(cooldownMaxMillis, cooldownRandomSeed)

      val setupMessage = Setup[Command, SetupProps, SetupSuccessProps, SetupFailureProps](
        props = SetupProps(
          nodeId,
          cooldownRandomSeed,
          cooldownMaxMillis
        ),
        replyTo = setupReplyProbe.ref
      )

      node ! setupMessage
      unhandledProbe.expectNoMessage()
      leftProbe.expectNoMessage()
      rightProbe.expectNoMessage()
      val setupReply = setupReplyProbe.receiveMessage()
      setupReply should matchPattern {
        case Right((_: ActorRef[Command], SetupSuccessProps(i))) if nodeId == i =>
      }

      node ! setupMessage
      val unhandled = unhandledProbe.receiveMessage()
      unhandled.message should ===(setupMessage)
      unhandledProbe.expectNoMessage()

      manualTime.timePasses(expectedCooldown.next())
      leftProbe.expectNoMessage()
      rightProbe.expectNoMessage()
    }

    "propagate message" in {
      val circleNodeSetup = spawn(CircleNode())
      type SetupReply = Either[SetupFailureProps, (ActorRef[Command], SetupSuccessProps)]

      val unhandledProbe = createUnhandledMessageProbe()
      val leftProbe = createTestProbe[Command]()
      val rightProbe = createTestProbe[Command]()
      val setupReplyProbe = createTestProbe[SetupReply]()

      val nodeId = 1
      val cooldownRandomSeed = 1L
      val cooldownMaxMillis = 100L
      // guessing what will happen timing-wise
      val nodeCooldown = Cooldown(cooldownMaxMillis, cooldownRandomSeed)

      // setup
      val setupMessage = Setup[Command, SetupProps, SetupSuccessProps, SetupFailureProps](
        props = SetupProps(
          nodeId,
          cooldownRandomSeed,
          cooldownMaxMillis
        ),
        replyTo = setupReplyProbe.ref
      )
      circleNodeSetup ! setupMessage

      // Get circleNode
      val Right((circleNode, SetupSuccessProps(id))) = setupReplyProbe.receiveMessage()
      id should ===(nodeId)
      circleNode should not be null

      // set peers
      circleNode ! SetPeers(leftProbe.ref, rightProbe.ref)

      // Send SendMessage
      val fromProbe = createTestProbe[Command]()
      val sendMessage = SendMessage(from = fromProbe.ref, message = "hello")
      circleNode ! sendMessage
      fromProbe.expectNoMessage()

      // "from" is the propagating actor
      val expectedMessage = SendMessage(
        from = circleNode,
        message = sendMessage.message
      )

      val cooldown = nodeCooldown.next()
      manualTime.timePasses(cooldown - 1.milli)
      rightProbe.expectNoMessage()
      leftProbe.expectNoMessage()
      manualTime.timePasses(1.milli)
      rightProbe.expectMessage(expectedMessage)
      leftProbe.expectMessage(expectedMessage)

      setupReplyProbe.expectNoMessage()
      rightProbe.expectNoMessage()
      leftProbe.expectNoMessage()
      unhandledProbe.expectNoMessage()
    }

    "print self ID when received message from both neighbours" in {
      val circleNodeSetup = spawn(CircleNode())
      type SetupReply = Either[SetupFailureProps, (ActorRef[Command], SetupSuccessProps)]

      val unhandledProbe = createUnhandledMessageProbe()
      val leftProbe = createTestProbe[Command]()
      val rightProbe = createTestProbe[Command]()
      val setupReplyProbe = createTestProbe[SetupReply]()

      val nodeId = 1
      val cooldownRandomSeed = 1L
      val cooldownMaxMillis = 100L
      // guessing what will happen timing-wise
      val nodeCooldown = Cooldown(cooldownMaxMillis, cooldownRandomSeed)

      // setup
      val setupMessage = Setup[Command, SetupProps, SetupSuccessProps, SetupFailureProps](
        props = SetupProps(
          nodeId,
          cooldownRandomSeed,
          cooldownMaxMillis
        ),
        replyTo = setupReplyProbe.ref
      )
      circleNodeSetup ! setupMessage

      // Get circleNode
      val Right((circleNode, SetupSuccessProps(id))) = setupReplyProbe.receiveMessage()
      id should ===(nodeId)
      circleNode should not be null

      // set peers
      circleNode ! SetPeers(leftProbe.ref, rightProbe.ref)

      // Send SendMessages
      val sendMessageFromLeft = SendMessage(from = leftProbe.ref, message = "hello")
      val sendMessageFromRight = SendMessage(from = rightProbe.ref, message = "hello")
      circleNode ! sendMessageFromLeft
      circleNode ! sendMessageFromRight

      val cooldown = List(nodeCooldown.next(), nodeCooldown.next()).min
      LoggingTestKit.info(s"$nodeId").expect {
        manualTime.timePasses(cooldown)
      }

      setupReplyProbe.expectNoMessage()
      rightProbe.expectNoMessage()
      leftProbe.expectNoMessage()
      unhandledProbe.expectNoMessage()
    }
  }
}
