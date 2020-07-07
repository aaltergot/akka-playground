package pg.circle

import akka.actor.testkit.typed.scaladsl.{LogCapturing, ScalaTestWithActorTestKit}
import akka.actor.typed.ActorRef
import org.scalatest.wordspec.AnyWordSpecLike
import pg.setup.Setup


class CircleNodeSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with LogCapturing {

  import CircleNode._

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

      val setupMessage = Setup[Command, SetupProps, SetupSuccessProps, SetupFailureProps](
        props = SetupProps(
          nodeId,
          cooldownRandomSeed = 1L,
          cooldownMaxMillis = 1L,
          left = leftProbe.ref,
          right = rightProbe.ref,
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

      // setup
      val setupMessage = Setup[Command, SetupProps, SetupSuccessProps, SetupFailureProps](
        props = SetupProps(
          nodeId,
          cooldownRandomSeed = 1L,
          cooldownMaxMillis = 1L,
          left = leftProbe.ref,
          right = rightProbe.ref,
        ),
        replyTo = setupReplyProbe.ref
      )
      circleNodeSetup ! setupMessage

      // Get circleNode
      val Right((circleNode, SetupSuccessProps(id))) = setupReplyProbe.receiveMessage()
      id should ===(nodeId)
      circleNode should not be null

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
      leftProbe.expectMessage(expectedMessage)
      rightProbe.expectMessage(expectedMessage)

      setupReplyProbe.expectNoMessage()
      rightProbe.expectNoMessage()
      leftProbe.expectNoMessage()
      unhandledProbe.expectNoMessage()
    }
  }
}
