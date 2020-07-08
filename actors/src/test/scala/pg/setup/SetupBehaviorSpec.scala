package pg.setup

import akka.actor.testkit.typed.scaladsl.{LogCapturing, ScalaTestWithActorTestKit}
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest.wordspec.AnyWordSpecLike

class SetupBehaviorSpec
  extends ScalaTestWithActorTestKit
  with AnyWordSpecLike
  with LogCapturing {

  "SetupBehaviour actor" must {
    "setup once" in {
      import SetupTestBehavior._

      val setupReplyProbe = createTestProbe[
        Either[SetupFailureProps, (ActorRef[InitializingCommand], SetupSuccessProps)]
      ]()

      val initializedProbe = createTestProbe[InitializingCommand]()

      val setupSuccessResult = Right((initializedProbe.ref, new SetupSuccessProps()))
      val setupSuccessBehavior = Behaviors.setup[
        Setup[InitializingCommand, SetupProps, SetupSuccessProps, SetupFailureProps]
      ] { ctx => new SetupTestBehavior(ctx, setupSuccessResult) }

      val testSetup = spawn(setupSuccessBehavior)

      val setupMessage =
        Setup[InitializingCommand, SetupProps, SetupSuccessProps, SetupFailureProps](
          props = new SetupProps,
          replyTo = setupReplyProbe.ref
        )

      testSetup ! setupMessage
      setupReplyProbe.receiveMessage() should ===(setupSuccessResult)

      testSetup ! setupMessage
      setupReplyProbe.expectNoMessage()
    }
  }
}

