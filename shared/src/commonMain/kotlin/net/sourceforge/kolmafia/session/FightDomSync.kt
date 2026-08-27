package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Orchestrates fight damage/verse/mode/comment syncs for
 * [net.sourceforge.kolmafia.adventure.AdventureManager.resolveCombat]
 * (Phases 1416–1430) plus processP / node / session-log (Phases 1611–1670).
 *
 * Order: modes → damage → verse → comments → processP → node → session-log
 * → (caller continues with cost/IoTM/final/HP).
 */
object FightDomSync {

    data class Context(
        val html: String,
        val adventureId: String = "",
        val locationName: String = "",
        val activeEffects: Collection<String> = emptyList(),
        val inPokefam: Boolean = false,
        val isFightStart: Boolean = false,
        val character: KoLCharacter? = null,
        val preferences: Preferences? = null,
        val inventory: InventoryManager? = null,
        val won: Boolean = false,
        val lost: Boolean = false,
        val fightEnded: Boolean = false,
        val sessionLogger: SessionLogger? = null,
        val effectManager: EffectManager? = null,
    )

    fun apply(ctx: Context): Boolean {
        var changed = false
        FightCombatModeSync.applyFightHtml(
            html = ctx.html,
            adventureId = ctx.adventureId,
            locationName = ctx.locationName,
            activeEffects = ctx.activeEffects,
            inPokefam = ctx.inPokefam,
            isFightStart = ctx.isFightStart,
        )
        changed = true

        if (FightCombatModeSync.isGarbled) {
            changed = FightVerseSync.apply(ctx.html, ctx.character) || changed
            if (!FightCombatModeSync.machineElf) {
                changed = FightDamageParser.apply(ctx.html) || changed
            }
        } else {
            changed = FightDamageParser.apply(ctx.html) || changed
            changed = FightVerseSync.apply(ctx.html, ctx.character) || changed
        }

        changed = FightCommentSync.apply(ctx.html, ctx.preferences) || changed

        val mildProfessor = ctx.character?.state?.value
            ?.isMildManneredProfessor(ctx.activeEffects) == true
        changed = FightProcessPSync.apply(
            html = ctx.html,
            preferences = ctx.preferences,
            won = ctx.won,
            sessionLogger = ctx.sessionLogger,
            mildManneredProfessor = mildProfessor,
        ) || changed

        changed = FightNodeSync.apply(
            html = ctx.html,
            preferences = ctx.preferences,
            inventory = ctx.inventory,
            character = ctx.character,
            effectManager = ctx.effectManager,
            sessionLogger = ctx.sessionLogger,
        ) || changed

        changed = FightSessionLog.apply(
            html = ctx.html,
            sessionLogger = ctx.sessionLogger,
            won = ctx.won,
            fightEnded = ctx.fightEnded,
            monsterName = MonsterStatusTracker.getLastMonsterName(),
            preferences = ctx.preferences,
        ) || changed

        return changed
    }

    fun resetFight() {
        FightCombatModeSync.reset()
        FightCommentSync.reset()
    }
}
