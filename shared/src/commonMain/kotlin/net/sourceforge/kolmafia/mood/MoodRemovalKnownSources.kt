package net.sourceforge.kolmafia.mood

/** Desktop [MoodTrigger.knownSources]: actions learned from mood `lose_effect` triggers. */
object MoodRemovalKnownSources {
    private val knownSources = mutableMapOf<String, LinkedHashSet<String>>()

    fun register(effectName: String, action: String) {
        if (effectName.isBlank() || action.isBlank()) return
        knownSources.computeIfAbsent(effectName) { LinkedHashSet() }.add(action)
    }

    fun getKnownSources(name: String): String {
        val existingActions = knownSources[name] ?: return ""
        return existingActions.joinToString("|")
    }

    fun clear() {
        knownSources.clear()
    }

    fun rebuildFromLibrary(moods: Collection<Mood>) {
        clear()
        for (mood in moods) {
            for (trigger in mood.removalTriggers) {
                if (trigger.type == MoodRemovalTriggerType.LOSE_EFFECT) {
                    register(trigger.effectName, trigger.action)
                }
            }
        }
    }
}
