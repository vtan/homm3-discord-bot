package h3bot

import scala.collection.convert.AsScalaExtensions
import scala.io.Source

import org.jsoup.Jsoup
import org.jsoup.nodes.{Element, TextNode}

object CreatureListScraper extends AsScalaExtensions {

  private val filename = "creatures.html"

  private def loadFile(): String =
    Source.fromFile(filename).mkString

  def scrapeCreatures: Seq[Creature] = {
    val document = Jsoup.parse(loadFile())
    document.select("tr").asScala.flatMap(scrapeCreature).toSeq
  }

  def scrapeCreature(row: Element): Option[Creature] = {
    val cells = row.children.asScala.filter(_.tag.getName == "td").toList
    cells match {
      case List(
        nameEl, townEl, levelEl, attackEl, defenseEl, damageMinEl, damageMaxEl, hpEl,
        speedEl, growthEl, valueEl, costGoldEl, costOtherEl, specialEl
      ) =>
        for {
          name <- scrapeString(nameEl)
          townName <- scrapeStringWithTitles(townEl)
          levelWithSub <- scrapeString(levelEl)
          level <- levelWithSub.take(1).toIntOption
          subLevel = levelWithSub.count(_ == '+')
          attack <- scrapeInt(attackEl)
          defense <- scrapeInt(defenseEl)
          damageMin <- scrapeInt(damageMinEl)
          damageMax <- scrapeInt(damageMaxEl)
          hp <- scrapeInt(hpEl)
          speed <- scrapeInt(speedEl)
          growth <- scrapeInt(growthEl)
          value <- scrapeInt(valueEl)
          costGold <- scrapeStringWithTitles(costGoldEl)
          costOther <- scrapeStringWithTitles(costOtherEl)
          cost = (costGold ++ costOther).trim
          special <- scrapeString(specialEl)
        } yield Creature(name, townName, level, subLevel, attack, defense, damageMin, damageMax, hp, speed, growth, value, cost, special)
      case _ =>
        None
    }
  }

  private def scrapeString(el: Element): Option[String] = Some(el.text)

  private def scrapeInt(el: Element): Option[Int] = el.text.toIntOption

  private def scrapeStringWithTitles(el: Element): Option[String] =
    Some(
      el.childNodes.asScala.collect {
        case n: TextNode => n.text
        case n: Element => n.attr("title").trim
      }.mkString("")
    ).filter(_.nonEmpty)
}
