package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/** Recognizes the beard effect granted by the Daylight Shavings Helmet. */
object DaylightShavingsHelmetManager {
    val messages = listOf(
        "Your helmet shoots some lasers at your face.  You smell burning hair.",
        "A pair of scissors emerges from somewhere in your helmet and adjusts your facial hair.",
        "A nozzle emerges from your helmet and sprays a pattern of depilatory foam on your face.",
        "A couple of straight razors snake out of a panel on the side of your helmet and quickly give you a shave.",
        "A clippers-tipped robotic arm emerges from your helmet and gives you a quick face trim.",
    )

    private val effectPattern = Regex("""onClick=['"]eff\(["'](.*?)["']\);['"]""", RegexOption.IGNORE_CASE)

    fun updatePreference(
        responseText: String?,
        preferences: Preferences?,
        itemCount: (Int) -> Int = { 0 },
        equipped: Boolean = true,
    ): Boolean {
        if (responseText.isNullOrBlank() || preferences == null || !equipped) return false
        val messageStart = messages.asSequence()
            .map { responseText.indexOf(it) }
            .filter { it >= 0 }
            .minOrNull() ?: return false
        val effectDescription = effectPattern.find(responseText, messageStart)
            ?.groupValues?.getOrNull(1) ?: return false
        val effect = EffectDatabase.getByDescId(effectDescription)?.id ?: return false
        if (effect !in 2666..2676) return false
        preferences.setInt("lastBeardBuff", effect)
        return true
    }
}
