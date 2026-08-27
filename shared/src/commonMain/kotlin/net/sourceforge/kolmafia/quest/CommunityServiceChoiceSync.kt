package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop Community Service choice 1089 completion ledger. */
object CommunityServiceChoiceSync {
    const val CHOICE_ID = 1089
    private val services = listOf(
        "Donate Blood", "Feed The Children", "Build Playground Mazes", "Feed Conspirators",
        "Breed More Collies", "Reduce Gazelle Population", "Make Sausage", "Be a Living Statue",
        "Make Margaritas", "Clean Steam Tunnels", "Coil Wire",
    )

    fun apply(choiceId: Int, decision: Int, html: String, preferences: Preferences?): Boolean {
        if (choiceId != CHOICE_ID || !html.contains("You acquire") || preferences == null) return false
        val service = services.getOrNull(decision - 1) ?: return false
        val current = preferences.getString("csServicesPerformed", "")
        preferences.setString("csServicesPerformed", if (current.isEmpty()) service else "$current,$service")
        return true
    }
}
