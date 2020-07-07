package pg.setup

import akka.actor.testkit.typed.scaladsl.{LogCapturing, ScalaTestWithActorTestKit}
import org.scalatest.wordspec.AnyWordSpecLike

class SetupBehaviorSpec
  extends ScalaTestWithActorTestKit
  with AnyWordSpecLike
  with LogCapturing {

  "SetupBehaviour actor" must {
    "asdf" in {
      val test = new TestBehavior()
      val testSetup = spawn(test)

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

      testSetup ! Setup()
    }
  }
}


object InitializingCommand
object SetupProps
object SetupSuccessProps
object SetupFailureProps

class TestBehavior
  extends SetupBehavior[
    InitializingCommand,
    SetupProps,
    SetupSuccessProps,
    SetupFailureProps
  ] {


}
