package pg.chaintravel

import akka.actor.testkit.typed.scaladsl.{LogCapturing, LoggingTestKit, ScalaTestWithActorTestKit}
import org.scalatest.wordspec.AnyWordSpecLike

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

            setupReplyProbe.receiveMessage()
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

            // prints hops count
            LoggingTestKit.info("10").expect {
                visitRef ! Visit(10, Seq(HopInfo(1, 1)))
            }

            // prints hops history
            LoggingTestKit.info("actor 1, message received 1").expect {
                visitRef ! Visit(10, Seq(HopInfo(1, 1), HopInfo(2, 2)))
            }
            LoggingTestKit.info("actor 2, message received 2").expect {
                visitRef ! Visit(10, Seq(HopInfo(1, 1), HopInfo(2, 2)))
            }

            // also prints self HopInfo
            LoggingTestKit.info(s"actor $id, message received").expect {
                visitRef ! Visit(10, Seq(HopInfo(1, 1)))
            }

            setupReplyProbe.expectNoMessage()
        }
    }

}
