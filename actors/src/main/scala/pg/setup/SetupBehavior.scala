package pg.setup

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}


case class Setup[InitializingCommand, SetupProps, SetupSuccessProps, SetupFailureProps](
  props: SetupProps,
  replyTo: ActorRef[Either[SetupFailureProps, (ActorRef[InitializingCommand], SetupSuccessProps)]]
)

abstract class SetupBehavior[InitializingCommand, SetupProps, SetupSuccessProps, SetupFailureProps](
  context: ActorContext[Setup[InitializingCommand, SetupProps, SetupSuccessProps, SetupFailureProps]]
) extends AbstractBehavior[Setup[InitializingCommand, SetupProps, SetupSuccessProps, SetupFailureProps]](context) {

  override def onMessage(
    msg: Setup[InitializingCommand, SetupProps, SetupSuccessProps, SetupFailureProps]
  ): Behavior[Setup[InitializingCommand, SetupProps, SetupSuccessProps, SetupFailureProps]] = {
    msg.replyTo ! setup(msg.props)
    Behaviors.empty
  }

  protected def setup(props: SetupProps): Either[SetupFailureProps, (ActorRef[InitializingCommand], SetupSuccessProps)]
}

object SetupBehavior {

  def decorate[InitializingCommand, SetupProps, SetupSuccessProps, SetupFailureProps](
    setupF: SetupProps => Either[SetupFailureProps, (ActorRef[InitializingCommand], SetupSuccessProps)]
  ): Behavior[Setup[InitializingCommand, SetupProps, SetupSuccessProps, SetupFailureProps]] =
    Behaviors.setup(context =>
      new SetupBehavior[InitializingCommand, SetupProps, SetupSuccessProps, SetupFailureProps](
        context = context
      ) {

        override protected def setup(
          props: SetupProps
        ): Either[
          SetupFailureProps,
          (ActorRef[InitializingCommand], SetupSuccessProps)
        ] = setupF(props)
      })
}
