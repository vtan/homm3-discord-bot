package h3bot

import scala.annotation.tailrec

final case class CreatureList(creatures: Vector[Creature]) extends AnyVal {

  def search(name: String): Option[Creature] = {
    val needle = name.trim.toLowerCase

    val exactMatch = creatures.find(_.searchKey == needle)

    lazy val prefixMatch = creatures.find { creature =>
      creature.searchWords.exists(_ startsWith needle)
    }

    lazy val levenshteinMatch = creatures.minByOption { creature =>
      (creature.searchKey +: creature.searchWords)
        .map(levenshteinDistance(needle, _))
        .min
    }

    exactMatch orElse prefixMatch orElse levenshteinMatch
  }

  @tailrec
  private def levenshteinDistance(a: String, b: String): Int =
    if (a.length > b.length) {
      levenshteinDistance(b, a)
    } else {
      val m = a.length
      val n = b.length
      var v0 = (0 to n).toArray
      var v1 = Array.fill(n + 1)(0)

      for (i <- 0 until m) {
        v1(0) = i + 1

        for (j <- 0 until n) {
          val deletionCost = v0(j + 1) + 1
          val insertionCost = v1(j) + 1
          val substitutionCost = if (a(i) == b(j)) v0(j) else v0(j) + 1
          v1(j + 1) = List(deletionCost, insertionCost, substitutionCost).min
        }

        val tmp = v0
        v0 = v1
        v1 = tmp
      }

      v0(n)
    }
}
