package h3bot

final case class Creature(
  name: String,
  townName: String,
  level: Int,
  subLevel: Int,
  attack: Int,
  defense: Int,
  damageMin: Int,
  damageMax: Int,
  hp: Int,
  speed: Int,
  growth: Int,
  value: Int,
  cost: String,
  special: String
) {
  val searchKey: String = name.toLowerCase
  val searchWords: Array[String] = searchKey.split("\\s+")
}
