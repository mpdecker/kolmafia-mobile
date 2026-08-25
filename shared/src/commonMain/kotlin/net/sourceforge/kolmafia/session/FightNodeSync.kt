package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop processNode structural residue (Phases 1641–1655): CLEESH transform,
 * MONSTERID id-change transform, mid-fight `rel=` item tables.
 */
object FightNodeSync {

    private val CLEESH = Regex(
        """newpic\(\s*["'][^"']*["']\s*,\s*["']([^"']+)["']""",
        RegexOption.IGNORE_CASE,
    )
    private val REL_ITEM = Regex(
        """<table[^>]*class=["']item["'][^>]*rel=["']([^"']+)["'][^>]*>""",
        RegexOption.IGNORE_CASE,
    )
    private val REL_ID = Regex("""(?:^|&)id=(\d+)""")

    fun apply(
        html: String,
        preferences: Preferences? = null,
        inventory: InventoryManager? = null,
        character: KoLCharacter? = null,
        effectManager: EffectManager? = null,
        sessionLogger: SessionLogger? = null,
    ): Boolean {
        if (html.isBlank()) return false
        var changed = false
        changed = applyCleesh(html, preferences, sessionLogger) || changed
        changed = applyRelItems(html, preferences, inventory, character, effectManager) || changed
        return changed
    }

    fun applyCleesh(
        html: String,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
    ): Boolean {
        val m = CLEESH.find(html) ?: return false
        // Require script context to avoid false positives
        if (!html.contains("<script", ignoreCase = true) &&
            !html.contains("newpic(", ignoreCase = true)
        ) {
            return false
        }
        val monsterName = m.groupValues[1].trim()
        if (monsterName.isEmpty()) return false
        FightSessionLog.logText("your opponent becomes $monsterName!", sessionLogger)
        preferences?.setString(Preferences.LAST_MONSTER, monsterName)
        val def = MonsterDatabase.getByName(monsterName)
        if (def != null) {
            MonsterStatusTracker.setNextMonster(def, emptyList())
        } else {
            MonsterStatusTracker.resetLastMonster()
            // Keep name visible for ASH even without a definition
            preferences?.setString(Preferences.LAST_MONSTER, monsterName)
        }
        return true
    }

    /**
     * When MONSTERID changes mid-fight, transform tracker to the new monster
     * (desktop [FightRequest.processComment] transform path).
     */
    fun transformMonsterId(
        previousId: Int?,
        newId: Int,
        preferences: Preferences?,
    ): Boolean {
        if (previousId != null && previousId == newId) return false
        val def = MonsterDatabase.getById(newId) ?: return false
        MonsterStatusTracker.setNextMonster(def, emptyList())
        preferences?.setString(Preferences.LAST_MONSTER, def.name)
        return true
    }

    fun applyRelItems(
        html: String,
        preferences: Preferences?,
        inventory: InventoryManager?,
        character: KoLCharacter?,
        effectManager: EffectManager?,
    ): Boolean {
        var changed = false
        for (m in REL_ITEM.findAll(html)) {
            val rel = m.groupValues[1]
            val id = REL_ID.find(rel)?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: rel.toIntOrNull()
                ?: continue
            if (id <= 0) continue
            val name = ItemDatabase.getById(id)?.name ?: continue
            // Avoid double-counting if ResultProcessor already saw "You acquire an item"
            if (html.contains("You acquire an item", ignoreCase = true) &&
                html.contains(name, ignoreCase = true)
            ) {
                continue
            }
            inventory?.gainItemLocally(id, 1)
            ResultProcessor.gainItem(
                adventureResults = true,
                itemId = id,
                count = 1,
                itemName = name,
                preferences = preferences,
            )
            changed = true
        }
        return changed
    }
}
