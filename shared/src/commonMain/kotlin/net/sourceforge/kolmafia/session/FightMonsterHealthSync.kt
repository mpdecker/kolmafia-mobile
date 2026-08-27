package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.combat.MonsterStatusTracker

/**
 * Desktop [FightRequest.updateMonsterHealth] regex subset (Phases 1301–1310):
 * detective skull, toy space helmet, and Monster Manuel table cells.
 */
object FightMonsterHealthSync {

    private val DETECTIVE = Regex(
        """I deduce that this monster has approximately (\d+) hit points""",
    )
    private val SPACE_HELMET = Regex("""Opponent HP: (\d+)""")
    private val NS_ML = Regex(
        """The Sorceress pauses for a moment, mutters some words under her breath, and straightens out her dress\. Her skin seems to shimmer for a moment\.""",
    )

    private val MANUEL_ATTACK = Regex(
        """Enemy's Attack Power[\s\S]*?<td[^>]*>\s*([\d,]+)\s*</td>""",
        RegexOption.IGNORE_CASE,
    )
    private val MANUEL_DEFENSE = Regex(
        """Enemy's Defense[\s\S]*?<td[^>]*>\s*([\d,]+)\s*</td>""",
        RegexOption.IGNORE_CASE,
    )
    private val MANUEL_HP = Regex(
        """Enemy's Hit Points[\s\S]*?<td[^>]*>\s*([\d,]+)\s*</td>""",
        RegexOption.IGNORE_CASE,
    )

    /** Simpler Manuel block: alt attributes adjacent to numeric cells. */
    private val MANUEL_ALT_BLOCK = Regex(
        """alt="Enemy's (Attack Power|Defense|Hit Points)"[\s\S]{0,200}?>([\d,]+)<""",
        RegexOption.IGNORE_CASE,
    )

    fun apply(html: String): Boolean {
        if (html.isBlank()) return false
        var changed = false
        if (NS_ML.containsMatchIn(html)) {
            MonsterStatusTracker.resetAttackAndDefense()
            changed = true
        }
        DETECTIVE.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { hp ->
            setHealthEstimate(hp)
            changed = true
        }
        SPACE_HELMET.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { hp ->
            setHealthEstimate(hp)
            changed = true
        }
        changed = applyManuel(html) || changed
        return changed
    }

    fun applyManuel(html: String): Boolean {
        var attack = 0
        var defense = 0
        var hp = 0
        MANUEL_ALT_BLOCK.findAll(html).forEach { m ->
            val value = m.groupValues[2].replace(",", "").toIntOrNull() ?: return@forEach
            when (m.groupValues[1].lowercase()) {
                "attack power" -> attack = value
                "defense" -> defense = value
                "hit points" -> hp = value
            }
        }
        if (attack == 0) {
            MANUEL_ATTACK.find(html)?.groupValues?.getOrNull(1)?.replace(",", "")
                ?.toIntOrNull()?.let { attack = it }
        }
        if (defense == 0) {
            MANUEL_DEFENSE.find(html)?.groupValues?.getOrNull(1)?.replace(",", "")
                ?.toIntOrNull()?.let { defense = it }
        }
        if (hp == 0) {
            MANUEL_HP.find(html)?.groupValues?.getOrNull(1)?.replace(",", "")
                ?.toIntOrNull()?.let { hp = it }
        }
        if (attack == 0 && defense == 0 && hp == 0) return false
        MonsterStatusTracker.setManuelStats(attack, defense, hp)
        MonsterStatusTracker.applyManuelStats()
        return true
    }

    private fun setHealthEstimate(hp: Int) {
        if (MonsterStatusTracker.getLastMonster() == null) return
        val current = MonsterStatusTracker.getMonsterHealth()
        if (current > hp) {
            MonsterStatusTracker.damageMonster(current - hp)
        } else if (hp > current) {
            MonsterStatusTracker.healMonster(hp - current)
        }
    }
}
