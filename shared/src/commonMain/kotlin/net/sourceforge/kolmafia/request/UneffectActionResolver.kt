package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BreakfastManager

/** Desktop [UneffectRequest.getAction] routing for mobile uneffect/CLI. */
sealed class UneffectAction {
    data class CastSkill(val skillName: String) : UneffectAction()
    data object HotTub : UneffectAction()
    data class UseItem(val itemId: Int, val retrieveFirst: Boolean = false) : UneffectAction()
    data object HttpUneffect : UneffectAction()
}

data class UneffectActionContext(
    val effectId: Int,
    val effectName: String = "",
    val moodPredefinedAction: String? = null,
    val preferences: Preferences?,
    val characterState: CharacterState?,
    val hasItemId: (Int) -> Boolean,
    val hasSkill: (String) -> Boolean,
    val canCastSkill: (String) -> Boolean,
    val canRetrieveRemedy: Boolean = false,
    val canAcquireUneffectItem: (Int) -> Boolean = { false },
)

object UneffectActionResolver {

    fun resolve(ctx: UneffectActionContext): UneffectAction {
        resolveMoodPredefinedAction(ctx)?.let { return it }

        val skillName = UneffectRemovableMaps.getUneffectSkill(ctx.effectId, ctx.hasSkill)
        if (skillName.isNotEmpty() && ctx.canCastSkill(skillName)) {
            return UneffectAction.CastSkill(skillName)
        }

        if (UneffectRemovableMaps.removableByShakeItOff(ctx.effectId) && canUseHotTub(ctx)) {
            return UneffectAction.HotTub
        }

        UneffectRemovableMaps.getUneffectItemId(ctx.effectId)?.let { itemId ->
            if (ctx.hasItemId(itemId)) {
                return UneffectAction.UseItem(itemId)
            }
            if (ctx.canAcquireUneffectItem(itemId)) {
                return UneffectAction.UseItem(itemId, retrieveFirst = true)
            }
        }

        if (ctx.hasItemId(UneffectRemovableMaps.ANCIENT_CURE_ALL)) {
            return UneffectAction.UseItem(UneffectRemovableMaps.ANCIENT_CURE_ALL)
        }
        if (ctx.hasItemId(UneffectRemovableMaps.REMEDY)) {
            return UneffectAction.UseItem(UneffectRemovableMaps.REMEDY)
        }
        if (ctx.canRetrieveRemedy) {
            return UneffectAction.UseItem(UneffectRemovableMaps.REMEDY, retrieveFirst = true)
        }
        return UneffectAction.HttpUneffect
    }

    internal fun resolveMoodPredefinedAction(ctx: UneffectActionContext): UneffectAction? {
        var action = ctx.moodPredefinedAction?.trim().orEmpty()
        if (action.isEmpty()) return null

        val skillName = when {
            action.startsWith("cast ", ignoreCase = true) ->
                MoodUneffectActionParser.parseSkillFromCastAction(action)
            action.startsWith("skill ", ignoreCase = true) ->
                MoodUneffectActionParser.parseSkillFromCastAction(action)
            else -> null
        }
        if (skillName != null && !ctx.hasSkill(skillName)) {
            action = ""
        }

        if (action.isEmpty() || action.startsWith("uneffect ", ignoreCase = true)) {
            return null
        }

        return MoodUneffectActionParser.parse(action, ctx)
    }

    /** Desktop UneffectRequest.getAction hot tub gates. */
    internal fun canUseHotTub(ctx: UneffectActionContext): Boolean {
        val prefs = ctx.preferences ?: return false
        val state = ctx.characterState ?: return false
        if (!ctx.hasItemId(BreakfastManager.VIP_LOUNGE_KEY_ID)) return false
        if (ZodiacSign.find(state.zodiacSign)?.isBadMoon == true) return false
        if (prefs.getInt("_hotTubSoaks", 0) >= 5) return false
        if (!prefs.getBoolean("uneffectWithHotTub", true)) return false
        if (StoragePullRules.canInteract(state)) return false
        return true
    }
}
