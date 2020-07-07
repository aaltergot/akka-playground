package pg.chaintravel

import akka.actor.testkit.typed.scaladsl.{LogCapturing, ScalaTestWithActorTestKit}
import akka.actor.typed.ActorRef
import org.scalatest.wordspec.AnyWordSpecLike

class ChainNodeSpec
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with LogCapturing {

  import ChainNode._

  "ChainNode actor" must {

    "ignore WrappedVisit while uninitialized" in {
      val probe = createUnhandledMessageProbe()

      val chainNode: ActorRef[ChainNode.Command] = spawn(ChainNode())
      chainNode ! WrappedVisit(Visit(0, Seq.empty))

      probe.receiveMessage()
      probe.expectNoMessage()
    }

    "reply on setup" in {
      val stub = createTestProbe[Visit]()
      val setupReplyProbe = createTestProbe[SetupSuccess]()

      val chainNode = spawn(ChainNode())
      chainNode ! Setup(Settings(0, stub.ref), setupReplyProbe.ref)

      val ss: SetupSuccess = setupReplyProbe.receiveMessage()
      ss.nodeId should ===(0)
      setupReplyProbe.expectNoMessage()
      stub.expectNoMessage()
    }

    "iterate hops" in {
      val visitProbe = createTestProbe[Visit]()
      val setupReplyProbe = createTestProbe[SetupSuccess]()

      val chainNode = spawn(ChainNode())
      val id = 5
      chainNode ! Setup(Settings(id, visitProbe.ref), setupReplyProbe.ref)

      val ss: SetupSuccess = setupReplyProbe.receiveMessage()
      ss.nodeId should ===(id)
      ss.visitRef ! Visit(0, Seq.empty)

      val msg = visitProbe.receiveMessage()
      msg.numberOfHopsTravelled should ===(1)
      msg.hops.size should ===(1)
      msg.hops.headOption.map(_.nodeId) should ===(Some(id))
      visitProbe.expectNoMessage()
      setupReplyProbe.expectNoMessage()
    }
  }
}
