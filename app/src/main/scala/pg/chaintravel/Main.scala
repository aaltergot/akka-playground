package pg.chaintravel

import akka.actor.typed.ActorSystem
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Await
import scala.util.Random

object Main {

  private val log: Logger = LoggerFactory.getLogger(Main.getClass)
  private val maxRandomN = 100

  def main(args: Array[String]): Unit = {
    val n: Int = args.headOption.flatMap(_.toIntOption).getOrElse {
      val r = Random.nextInt(maxRandomN)
      log.info(s"Using random N=$r")
      r
    }

    val chainTravel = ActorSystem(ChainTravel(), "chaintravel")

    chainTravel ! ChainTravel.Travel(n)
  }
}
