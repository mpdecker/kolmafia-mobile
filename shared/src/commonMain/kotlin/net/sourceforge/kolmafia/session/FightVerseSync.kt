package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.combat.MonsterStatusTracker

/**
 * Desktop haiku / anapest / machineElf verse damage + phrase tables (Phases 1401–1415).
 */
object FightVerseSync {

    private val DAMAGE_TITLE = Regex(
        """title\s*=\s*["']Damage:\s*([^"']+)["']""",
        RegexOption.IGNORE_CASE,
    )
    private val INT = Regex("""(\d[\d,]*)""")

    private val HAIKU_HP_GAIN = listOf(
        "Your wounds fly away",
        "restored to your body",
        "You're feeling better",
    )
    private val ANAPEST_HP_GAIN = listOf(
        "You heal",
        "you heal",
        "feel a lot better",
        "added onto your score",
        "regenerate",
        "more hit points",
        "help clear up the lumps",
    )

    /**
     * Desktop [FightRequest.parseVerseDamage] — sum ints from title="Damage: …".
     * Skipped in machineElf (familiar-only titles).
     */
    fun parseVerseDamage(html: String, machineElf: Boolean = FightCombatModeSync.machineElf): Int {
        if (machineElf) return 0
        var total = 0
        DAMAGE_TITLE.findAll(html).forEach { m ->
            INT.findAll(m.groupValues[1]).forEach { n ->
                total += n.groupValues[1].replace(",", "").toIntOrNull() ?: 0
            }
        }
        return total
    }

    /** Haiku text-only: first int near "damage" / "from you to your foe". */
    fun parseHaikuDamage(html: String): Int {
        val text = html.replace(Regex("""<[^>]+>"""), " ")
        if (!text.contains("damage", ignoreCase = true) &&
            !text.contains("from you to your foe", ignoreCase = true)
        ) {
            return 0
        }
        return INT.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull() ?: 0
    }

    fun applyVerseDamage(html: String): Int {
        if (MonsterStatusTracker.getLastMonster() == null) return 0
        var damage = parseVerseDamage(html)
        if (damage == 0 && FightCombatModeSync.haiku) {
            damage = parseHaikuDamage(html)
        }
        if (damage > 0) {
            MonsterStatusTracker.damageMonster(damage)
        }
        return damage
    }

    data class VerseResult(
        val meat: Int = 0,
        val hpDelta: Int = 0,
        val mpDelta: Int = 0,
        val muscle: Int = 0,
        val mysticality: Int = 0,
        val moxie: Int = 0,
    )

    /**
     * Compact phrase/image recognition for garbled combat results.
     * Looks for img src + nearby bold ints.
     */
    fun parseGarbledResults(html: String): VerseResult {
        var meat = 0
        var hpDelta = 0
        var mpDelta = 0
        var muscle = 0
        var mysticality = 0
        var moxie = 0

        // meat.gif + int
        Regex(
            """meat\.gif[\s\S]{0,300}?<(?:b|font)[^>]*>\s*(\d[\d,]*)""",
            RegexOption.IGNORE_CASE,
        ).findAll(html).forEach {
            meat += it.groupValues[1].replace(",", "").toIntOrNull() ?: 0
        }

        // hp.gif
        Regex(
            """hp\.gif[\s\S]{0,400}?<(?:b|font)[^>]*>\s*(?:<font[^>]*>)?(\d[\d,]*)""",
            RegexOption.IGNORE_CASE,
        ).findAll(html).forEach { m ->
            val points = m.groupValues[1].replace(",", "").toIntOrNull() ?: return@forEach
            val window = html.substring(
                m.range.first,
                (m.range.last + 200).coerceAtMost(html.length),
            )
            val gain = when {
                FightCombatModeSync.machineElf ->
                    window.contains("you are healed", ignoreCase = true)
                FightCombatModeSync.haiku ->
                    HAIKU_HP_GAIN.any { window.contains(it) }
                FightCombatModeSync.anapest ->
                    ANAPEST_HP_GAIN.any { window.contains(it, ignoreCase = true) }
                else -> window.contains("gain", ignoreCase = true) ||
                    window.contains("heal", ignoreCase = true)
            }
            if (FightCombatModeSync.machineElf) {
                // Desktop logs "1 or more" without a precise number — skip numeric apply
            } else {
                hpDelta += if (gain) points else -points
            }
        }

        // mp.gif
        Regex(
            """mp\.gif[\s\S]{0,400}?<(?:b|font)[^>]*>\s*(?:<font[^>]*>)?(\d[\d,]*)""",
            RegexOption.IGNORE_CASE,
        ).findAll(html).forEach { m ->
            val points = m.groupValues[1].replace(",", "").toIntOrNull() ?: return@forEach
            val window = html.substring(
                m.range.first,
                (m.range.last + 200).coerceAtMost(html.length),
            )
            val gain = when {
                FightCombatModeSync.machineElf ->
                    window.contains("revitalizing you", ignoreCase = true)
                else -> !window.contains("lose", ignoreCase = true)
            }
            if (!FightCombatModeSync.machineElf) {
                mpDelta += if (gain) points else -points
            }
        }

        if (html.contains("strboost.gif")) muscle++
        if (html.contains("snowflakes.gif")) mysticality++
        if (html.contains("wink.gif")) moxie++

        // Machine elf meat from bold ints near meat
        if (FightCombatModeSync.machineElf && meat == 0) {
            Regex(
                """meat\.gif[\s\S]{0,200}?(\d[\d,]*)""",
                RegexOption.IGNORE_CASE,
            ).find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()?.let {
                meat = it
            }
        }

        return VerseResult(meat, hpDelta, mpDelta, muscle, mysticality, moxie)
    }

    fun applyGarbledToCharacter(html: String, character: KoLCharacter?): VerseResult {
        val result = parseGarbledResults(html)
        if (character == null) return result
        val s = character.state.value
        if (result.hpDelta != 0 || result.mpDelta != 0) {
            character.updateHpMp(
                (s.currentHp + result.hpDelta).coerceIn(0, s.maxHp.coerceAtLeast(0)),
                s.maxHp,
                (s.currentMp + result.mpDelta).coerceIn(0, s.maxMp.coerceAtLeast(0)),
                s.maxMp,
            )
        }
        if (result.meat != 0) {
            character.updateMeat(s.meat + result.meat)
        }
        if (result.muscle != 0 || result.mysticality != 0 || result.moxie != 0) {
            character.adjustSubstats(
                musDelta = result.muscle.toLong(),
                mysDelta = result.mysticality.toLong(),
                moxDelta = result.moxie.toLong(),
            )
        }
        return result
    }

    fun apply(html: String, character: KoLCharacter? = null): Boolean {
        if (!FightCombatModeSync.isGarbled && parseVerseDamage(html) == 0) {
            // Still try verse titles outside garbled modes (some IoTM use Damage: titles)
            val dmg = applyVerseDamage(html)
            return dmg > 0
        }
        var changed = false
        if (applyVerseDamage(html) > 0) changed = true
        if (FightCombatModeSync.isGarbled) {
            val r = applyGarbledToCharacter(html, character)
            if (r.meat != 0 || r.hpDelta != 0 || r.mpDelta != 0 ||
                r.muscle != 0 || r.mysticality != 0 || r.moxie != 0
            ) {
                changed = true
            }
        }
        return changed
    }
}
