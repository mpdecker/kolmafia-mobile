package net.sourceforge.kolmafia.character

/**
 * Parses disco momentum from fight HTML. Mirrors desktop [FightRequest] DISCO_MOMENTUM_PATTERN.
 */
object ClassResourceCombatSync {

    private val discoMomentumPattern = Regex("""discomo(\d)\.gif""")

    fun parseDiscoMomentum(html: String): Int? =
        discoMomentumPattern.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()

    fun apply(character: KoLCharacter, html: String) {
        parseDiscoMomentum(html)?.let { character.updateClassResource(discoMomentum = it) }
    }
}
