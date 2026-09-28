package net.sourceforge.kolmafia.webui

import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.maximizer.MaximizerPassiveSkills
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.modifiers.ModifierValues
import net.sourceforge.kolmafia.modifiers.StringModifier
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SpeculateHtml
import net.sourceforge.kolmafia.session.Speculation

/**
 * Desktop UseLinkDecorator getEquipmentSpeculation / getPotionSpeculation —
 * hover tooltips showing predicted modifier deltas.
 */
object UseLinkSpeculation {
    @Volatile
    var library: GameRuntimeLibrary? = null

    @Volatile
    private var sequence: Int = 0

    fun getEquipmentSpeculation(
        label: String,
        itemId: Int,
        slot: EquipmentSlot? = null,
        preferences: Preferences? = null,
    ): String {
        val state = liveState() ?: return label
        val itemName = ItemDatabase.getById(itemId)?.name ?: return label
        val resolved = slot ?: Speculation.chooseEquipmentSlot(itemId, state.equipment) ?: return label
        val effects = liveEffects()
        val passives = livePassives()
        val prefs = preferences ?: RelayServer.preferences
        val baseline = CurrentModifiers(
            state = state,
            activeEffects = effects,
            passiveSkillNames = passives,
            preferences = prefs,
        )
        val spec = Speculation.fromLive(state, effects, passives, prefs)
        spec.equip(resolved, itemName)
        val speculated = spec.calculate()
        return wrapLabel(label, SpeculateHtml.getHTML(speculated, baseline))
    }

    fun getPotionSpeculation(
        label: String,
        itemId: Int,
        preferences: Preferences? = null,
    ): String {
        val itemName = ItemDatabase.getById(itemId)?.name ?: return label
        val entry = ModifierDatabase.getItem(itemName) ?: return label
        val parsed = ModifierParser.parse(entry.modifiers)
        val effectName = parsed.get(StringModifier.EFFECT).orEmpty()
        if (effectName.isBlank()) return label
        val duration = parsed.get(DoubleModifier.EFFECT_DURATION).toInt()
        val effectDef = net.sourceforge.kolmafia.data.EffectDatabase.getByName(effectName) ?: return label
        val effect = EffectData(
            id = effectDef.id,
            name = effectDef.name,
            duration = maxOf(1, duration),
        )
        val state = liveState() ?: return label
        val effects = liveEffects()
        val passives = livePassives()
        val prefs = preferences ?: RelayServer.preferences
        val baseline = CurrentModifiers(
            state = state,
            activeEffects = effects,
            passiveSkillNames = passives,
            preferences = prefs,
        )
        val spec = Speculation.fromLive(state, effects, passives, prefs)
        spec.addEffect(effect)
        val speculated = spec.calculate()
        // Surface Effect / Effect Duration like desktop after calculate.
        val withMeta = speculated.values + ModifierValues(
            doubles = if (duration > 0) {
                mapOf(DoubleModifier.EFFECT_DURATION to duration.toDouble())
            } else {
                emptyMap()
            },
            strings = mapOf(StringModifier.EFFECT to listOf(effectName)),
        )
        val html = SpeculateHtml.getHTML(
            now = withMeta,
            nowDerived = speculated.derived,
            was = baseline.values,
            wasDerived = baseline.derived,
        )
        return wrapLabel(label, html)
    }

    private fun wrapLabel(label: String, table: String?): String {
        if (table == null) return label
        val id = "whatif${sequence++}"
        val positioned = table.replace(
            "<table border=2 ",
            "<table border=2 id='$id' style='background-color: white; visibility: hidden; " +
                "position: absolute; z-index: 1; right: 0px; top: 1.2em;' ",
        )
        return "<span style='position: relative;' " +
            "onMouseOver=\"document.getElementById('$id').style.visibility='visible';\" " +
            "onMouseOut=\"document.getElementById('$id').style.visibility='hidden';\">" +
            positioned + label + "</span>"
    }

    private fun liveState(): CharacterState? =
        (library ?: RelayServer.library)?.character?.state?.value

    private fun liveEffects(): List<EffectData> =
        (library ?: RelayServer.library)?.effectManager?.state?.value?.effects.orEmpty()

    private fun livePassives(): Set<String> {
        val skills = (library ?: RelayServer.library)?.skillManager?.state?.value?.skills.orEmpty()
        return MaximizerPassiveSkills.resolve(skills)
    }
}
