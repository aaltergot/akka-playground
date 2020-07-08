package pg.setup

import SetupTestBehavior._
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.ActorContext

class SetupTestBehavior(
  context: ActorContext[
    Setup[
      InitializingCommand,
      SetupProps,
      SetupSuccessProps,
      SetupFailureProps
    ]
  ],
  private val result: Either[SetupFailureProps, (ActorRef[InitializingCommand], SetupSuccessProps)]
)
    extends SetupBehavior[
      InitializingCommand,
      SetupProps,
      SetupSuccessProps,
      SetupFailureProps
    ](context) {

  override protected def setup(
    props: SetupProps
  ): Either[SetupFailureProps, (ActorRef[InitializingCommand], SetupSuccessProps)] = result
}

object SetupTestBehavior {
  trait InitializingCommand
  class SetupProps
  class SetupSuccessProps
  class SetupFailureProps
}
