package pg.chaintravel

import akka.actor.testkit.typed.scaladsl.{LogCapturing, LoggingTestKit, ScalaTestWithActorTestKit}
import org.scalatest.wordspec.AnyWordSpecLike
import org.slf4j.event.Level

class ChainTerminalSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with LogCapturing {

    import ChainTerminal._

    "ChainTerminal actor" must {

        "ignore WrappedVisit while uninitialized" in {
            val probe = createUnhandledMessageProbe()

            val chainTerminal = spawn(ChainTerminal())
            chainTerminal ! WrappedVisit(Visit(0, Seq.empty))

            probe.receiveMessage()
            probe.expectNoMessage()
        }

        "reply on setup" in {
            val setupReplyProbe = createTestProbe[SetupSuccess]()

            val chainTerminal = spawn(ChainTerminal())
            chainTerminal ! Setup(Settings(0), setupReplyProbe.ref)

            val ss: SetupSuccess = setupReplyProbe.receiveMessage()
            ss.nodeId should ===(0)
            setupReplyProbe.expectNoMessage()
        }

        "print results" in {
            val setupReplyProbe = createTestProbe[SetupSuccess]()

            val chainTerminal = spawn(ChainTerminal())
            val id = 5
            chainTerminal ! Setup(Settings(id), setupReplyProbe.ref)
            val ss = setupReplyProbe.receiveMessage()
            ss.nodeId should ===(id)
            val visitRef = ss.visitRef

            LoggingTestKit.empty
              .withMessageContains("10")
              .withMessageContains("actor 1, message received 1")
              .withMessageContains("actor 2, message received 2")
              .withMessageContains(s"actor $id, message received")
              .withLogLevel(Level.INFO)
              .expect {
                  visitRef ! Visit(10, Seq(HopInfo(1, 1)))
              }

            setupReplyProbe.expectNoMessage()
        }
    }

}
