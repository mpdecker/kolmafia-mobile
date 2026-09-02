package net.sourceforge.kolmafia.combat

import net.sourceforge.kolmafia.banish.Banisher

/** Fallback banisher resolution when fight HTML matched banish but not a specific source. */
object FightBanishSync {

    private val EXTRA_PATTERNS: List<Pair<Regex, Banisher>> = listOf(
        Regex("""knock your opponent into tomorrow""", RegexOption.IGNORE_CASE) to Banisher.ULTRA_HAMMER,
        Regex("""ensuing confusion""", RegexOption.IGNORE_CASE) to Banisher.SMOKE_GRENADE,
        Regex("""vortex disappears""", RegexOption.IGNORE_CASE) to Banisher.WALK_AWAY_FROM_EXPLOSION,
        Regex("""Right.*?Kick""", RegexOption.IGNORE_CASE) to Banisher.RIGHT_ZOOT_KICK,
        Regex("""Left.*?Kick""", RegexOption.IGNORE_CASE) to Banisher.LEFT_ZOOT_KICK,
        Regex("""as the vortex disappears""", RegexOption.IGNORE_CASE) to Banisher.WALK_AWAY_FROM_EXPLOSION,
        Regex("""B\. L\. A\. R\. T""", RegexOption.IGNORE_CASE) to Banisher.BLART_SPRAY_WIDE,
        Regex("""You burned that foe so hard""", RegexOption.IGNORE_CASE) to Banisher.THROWIN_EMBER,
        Regex("""deliver an epic punch""", RegexOption.IGNORE_CASE) to Banisher.PUNCH_OUT_YOUR_FOE,
        Regex("""release a majestic roar""", RegexOption.IGNORE_CASE) to Banisher.ROAR_LIKE_A_LION,
        Regex("""spew a heaping helping of your pheromones""", RegexOption.IGNORE_CASE) to Banisher.MARK_YOUR_TERRITORY,
        Regex("""bolt of lightning arcs out""", RegexOption.IGNORE_CASE) to Banisher.SEADENT_LIGHTNING,
        Regex("""Crimbuccaneer rigging lasso""", RegexOption.IGNORE_CASE) to Banisher.CRIMBUCCANEER_RIGGING_LASSO,
        Regex("""tryptophan dart""", RegexOption.IGNORE_CASE) to Banisher.TRYPTOPHAN_DART,
        Regex("""boring familiar pictures""", RegexOption.IGNORE_CASE) to Banisher.SHOW_YOUR_BORING_FAMILIAR_PICTURES,
        Regex("""standalone cheese""", RegexOption.IGNORE_CASE) to Banisher.STAFF_OF_THE_STANDALONE_CHEESE,
        Regex("""push away your opponent""", RegexOption.IGNORE_CASE) to Banisher.BE_A_MIND_MASTER,
        Regex("""skull explodes into a million""", RegexOption.IGNORE_CASE) to Banisher.CRYSTAL_SKULL,
        Regex("""throw the bell away""", RegexOption.IGNORE_CASE) to Banisher.HAROLDS_BELL,
        Regex("""peppermint bomb""", RegexOption.IGNORE_CASE) to Banisher.PEPPERMINT_BOMB,
        Regex("""tie up the gingerbread""", RegexOption.IGNORE_CASE) to Banisher.LICORICE_ROPE,
        Regex("""call in a favor from your mob""", RegexOption.IGNORE_CASE) to Banisher.ORDER_A_KNEECAPPING,
        Regex("""ear shattering screech""", RegexOption.IGNORE_CASE) to Banisher.PATRIOTIC_SCREECH,
        Regex("""ray blasts out of the stone""", RegexOption.IGNORE_CASE) to Banisher.HEARTSTONE_BANISH,
        Regex("""turns tail and runs""", RegexOption.IGNORE_CASE) to Banisher.STAFF_OF_THE_STANDALONE_CHEESE,
        Regex("""pass out from pure boredom""", RegexOption.IGNORE_CASE) to Banisher.SHOW_YOUR_BORING_FAMILIAR_PICTURES,
        Regex("""nozzle all the way and blast it out of sight""", RegexOption.IGNORE_CASE) to Banisher.BLART_SPRAY_WIDE,
    )

    fun resolveBanisher(html: String, current: Banisher): Banisher {
        if (current != Banisher.UNKNOWN) return current
        return EXTRA_PATTERNS.firstOrNull { (pattern, _) -> pattern.containsMatchIn(html) }?.second
            ?: Banisher.UNKNOWN
    }
}
