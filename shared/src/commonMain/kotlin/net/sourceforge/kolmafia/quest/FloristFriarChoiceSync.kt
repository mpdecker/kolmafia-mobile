package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop FloristRequest parsing for choice 720. */
object FloristFriarChoiceSync {
    const val CHOICE_ID = 720
    private val plants = mutableMapOf<String, MutableList<Int>>()

    fun reset() = plants.clear()
    fun plantsAt(location: String): List<Int> = plants[location]?.toList().orEmpty()

    fun apply(choiceId: Int, choiceUrl: String, html: String, preferences: Preferences?): Boolean {
        if (choiceId != CHOICE_ID) return false
        if (html.contains("The Florist Friar's Cottage")) preferences?.setBoolean("floristFriarAvailable", true)
        val option = choiceUrl.parameter("option")?.toIntOrNull() ?: 0
        val location = Regex("""Ah, <b>(.*?)</b>!""").find(html)?.groupValues?.get(1)
        when (option) {
            1 -> {
                val plant = choiceUrl.parameter("plant")?.toIntOrNull() ?: return true
                if (location != null && !html.contains("You need to dig up a space.") && !html.contains("Invalid plant")) {
                    plants.getOrPut(location) { mutableListOf() }.add(plant)
                }
            }
            2 -> if (location != null && html.contains("You dig up a plant.")) {
                val index = choiceUrl.parameter("plnti")?.toIntOrNull() ?: -1
                if (index in plantsAt(location).indices) plants[location]?.removeAt(index)
            }
            4 -> {
                plants.clear()
                Regex("""<tr><td>([^>]*?)</td><td width.*?plant(\d+)\.gif.*?(?:plant(\d+)\.gif)?.*?(?:plant(\d+)\.gif)?""")
                    .findAll(html).forEach { match ->
                        plants[match.groupValues[1]] = match.groupValues.drop(2).mapNotNull { it.toIntOrNull() }.toMutableList()
                    }
            }
        }
        return true
    }

    private fun String.parameter(name: String): String? =
        Regex("""(?:[?&])${Regex.escape(name)}=([^&]*)""").find(this)?.groupValues?.get(1)
}
