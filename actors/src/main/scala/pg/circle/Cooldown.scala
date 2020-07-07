package pg.circle

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.util.Random

/**
 * Represents a sequence of millisecond durations ([[FiniteDuration]])
 * in [1, maxMillis] interval.
 */
class Cooldown private (
  private val maxMillis: Long,
  private val random: Random
) {
  def next(): FiniteDuration = (random.nextLong(maxMillis) + 1).millis
}

object Cooldown {
  def apply(maxMillis: Long, randomSeed: Long): Cooldown =
    new Cooldown(maxMillis, new Random(randomSeed))
}
