package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.combat.MonsterStatusTracker

/**
 * Desktop [FightRequest.parseNormalDamage] + heal/delevel regex subset (Phases 1386–1400).
 */
object FightDamageParser {

    // Simplified but practical ports of desktop PHYSICAL/ELEMENTAL patterns
    private val PHYSICAL = Regex(
        """(?:^|[^\d])(\d[\d,]*) (?:\([^.]*\) )?(?:(?:[^\s]+ ){0,3})(?:damage|points?|hollow|notches?|to your opponent|force damage)""",
        RegexOption.IGNORE_CASE,
    )
    private val PHYSICAL_SIMPLE = Regex(
        """(?:dealing|for|doing|inflicts?)\s+(\d[\d,]*)\s+(?:\([^.]*\)\s+)?(?:damage|points?)""",
        RegexOption.IGNORE_CASE,
    )
    private val YOU_DEAL = Regex(
        """(?:You\s+(?:deal|hit|slash|smash|whack|punch|kick).*?(?:for|dealing)\s+)(\d[\d,]*)""",
        RegexOption.IGNORE_CASE,
    )
    private val ELEMENTAL = Regex(
        """\+?(\d[\d,]*) (?:\([^.]*\) )?(?:(?:slimy, (?:clammy|gross) |hotsy-totsy )?damage|points|HP worth)""",
        RegexOption.IGNORE_CASE,
    )
    private val SECONDARY = Regex("""\+(\d[\d,]*)""")
    private val IGNORE_PREFIX = Regex(
        """(?:You lose|You gain|stabs you for|your blood, to the tune of|strain your neck|approximately|roughly|sown)\s*#?\d""",
        RegexOption.IGNORE_CASE,
    )

    private val MONSTER_HEAL = Regex(
        """(?:regains?|heals?(?:\s+itself)?|looks about)\s+(?:for\s+)?(\d[\d,]*)\s*(?:hit points?|HP|health)?""",
        RegexOption.IGNORE_CASE,
    )
    private val ATTACK_DROP = Regex(
        """nicesword\.gif[\s\S]{0,200}?(\d[\d,]*)""",
        RegexOption.IGNORE_CASE,
    )
    private val DEFENSE_DROP = Regex(
        """whiteshield\.gif[\s\S]{0,200}?(\d[\d,]*)""",
        RegexOption.IGNORE_CASE,
    )
    private val ATTACK_DROP_TEXT = Regex(
        """(?:Attack|Power)\s+(?:Power\s+)?(?:drops?|falls?|decreases?)\s+(?:by\s+)?(\d[\d,]*)""",
        RegexOption.IGNORE_CASE,
    )
    private val DEFENSE_DROP_TEXT = Regex(
        """Defense\s+(?:drops?|falls?|decreases?)\s+(?:by\s+)?(\d[\d,]*)""",
        RegexOption.IGNORE_CASE,
    )

    /** Parse a single text block for physical/elemental damage against the monster. */
    fun parseNormalDamage(text: String): Int {
        if (text.isBlank()) return 0
        if (IGNORE_PREFIX.containsMatchIn(text)) return 0
        if (text.contains("Crimbuccaneer", ignoreCase = true)) return 0
        if (text.contains("shambles up", ignoreCase = true)) return 0
        if (text.contains("scroll to your opponent", ignoreCase = true)) return 0

        YOU_DEAL.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()?.let {
            return it + secondaryBonus(text)
        }
        PHYSICAL_SIMPLE.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()?.let {
            return it + secondaryBonus(text)
        }
        PHYSICAL.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()?.let {
            return it + secondaryBonus(text)
        }
        ELEMENTAL.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()?.let {
            return it + secondaryBonus(text)
        }
        return 0
    }

    private fun secondaryBonus(text: String): Int =
        SECONDARY.findAll(text).sumOf { it.groupValues[1].replace(",", "").toIntOrNull() ?: 0 }

    /**
     * Scan fight HTML for damage lines and apply to [MonsterStatusTracker].
     * Splits on tags / newlines to approximate text-node scanning.
     */
    fun applyDamageFromHtml(html: String): Int {
        if (html.isBlank() || MonsterStatusTracker.getLastMonster() == null) return 0
        var total = 0
        val blocks = html
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""<[^>]+>"""), "\n")
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        for (block in blocks) {
            val dmg = parseNormalDamage(block)
            if (dmg > 0) {
                MonsterStatusTracker.damageMonster(dmg)
                total += dmg
            }
        }
        return total
    }

    fun applyHealFromHtml(html: String): Int {
        if (MonsterStatusTracker.getLastMonster() == null) return 0
        var total = 0
        MONSTER_HEAL.findAll(html).forEach { m ->
            // Skip player heal phrases
            val ctx = html.substring(
                (m.range.first - 40).coerceAtLeast(0),
                (m.range.last + 1).coerceAtMost(html.length),
            )
            if (ctx.contains("You regain", ignoreCase = true) ||
                ctx.contains("You gain", ignoreCase = true) ||
                ctx.contains("You're feeling better", ignoreCase = true)
            ) {
                return@forEach
            }
            val amount = m.groupValues[1].replace(",", "").toIntOrNull() ?: return@forEach
            if (amount > 0) {
                MonsterStatusTracker.healMonster(amount)
                total += amount
            }
        }
        return total
    }

    fun applyDelevelFromHtml(html: String): Boolean {
        var changed = false
        ATTACK_DROP.find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()?.let {
            MonsterStatusTracker.lowerMonsterAttack(it)
            changed = true
        }
        DEFENSE_DROP.find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()?.let {
            MonsterStatusTracker.lowerMonsterDefense(it)
            changed = true
        }
        if (!changed) {
            ATTACK_DROP_TEXT.find(html)?.groupValues?.getOrNull(1)?.replace(",", "")
                ?.toIntOrNull()?.let {
                    MonsterStatusTracker.lowerMonsterAttack(it)
                    changed = true
                }
            DEFENSE_DROP_TEXT.find(html)?.groupValues?.getOrNull(1)?.replace(",", "")
                ?.toIntOrNull()?.let {
                    MonsterStatusTracker.lowerMonsterDefense(it)
                    changed = true
                }
        }
        return changed
    }

    fun apply(html: String): Boolean {
        var changed = false
        if (applyDamageFromHtml(html) > 0) changed = true
        if (applyHealFromHtml(html) > 0) changed = true
        if (applyDelevelFromHtml(html)) changed = true
        return changed
    }
}
