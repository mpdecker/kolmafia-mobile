package net.sourceforge.kolmafia.buffbot

/**
 * Regex XML parse for buffbot price lists (desktop [BuffBotDatabase.DynamicBotFetcher]).
 */
object BuffBotXmlParser {

    private val buffDataPattern = Regex("<buffdata>(.*?)</buffdata>", RegexOption.DOT_MATCHES_ALL)
    private val namePattern = Regex("<name>(.*?)</name>", RegexOption.DOT_MATCHES_ALL)
    private val pricePattern = Regex("<price>(.*?)</price>", RegexOption.DOT_MATCHES_ALL)
    private val turnPattern = Regex("<turns>(.*?)</turns>", RegexOption.DOT_MATCHES_ALL)
    private val freePattern = Regex("<philanthropic>(.*?)</philanthropic>", RegexOption.DOT_MATCHES_ALL)

    private const val JALAPENO_SAUCESPHERE = "Jalape\u00f1o Saucesphere"

    fun parse(xml: String, botName: String): Pair<List<BuffBotOffering>, List<BuffBotOffering>> {
        val philanthropic = mutableListOf<BuffBotOffering>()
        val standard = mutableListOf<BuffBotOffering>()

        for (nodeMatch in buffDataPattern.findAll(xml)) {
            val buffMatch = nodeMatch.groupValues[1]
            val nameMatch = namePattern.find(buffMatch) ?: continue
            val priceMatch = pricePattern.find(buffMatch) ?: continue
            val turnMatch = turnPattern.find(buffMatch) ?: continue

            var name = nameMatch.groupValues[1].trim()
            if (name.startsWith("Jala")) {
                name = JALAPENO_SAUCESPHERE
            }

            val price = priceMatch.groupValues[1].trim().toIntOrNull() ?: continue
            val turns = turnMatch.groupValues[1].trim().toIntOrNull() ?: continue
            val philanthropicFlag = freePattern.find(buffMatch)?.groupValues?.get(1)?.trim() == "true"

            val target = if (philanthropicFlag) philanthropic else standard
            val existing = target.find { it.price == price }
            if (existing == null) {
                target.add(
                    BuffBotOffering(
                        botName = botName,
                        price = price,
                        philanthropic = philanthropicFlag,
                        buffs = listOf(name),
                        turns = listOf(turns),
                    ),
                )
            } else {
                val index = target.indexOf(existing)
                target[index] = existing.addBuff(name, turns)
            }
        }

        philanthropic.sortBy { it.price }
        standard.sortBy { it.price }
        return philanthropic to standard
    }
}
