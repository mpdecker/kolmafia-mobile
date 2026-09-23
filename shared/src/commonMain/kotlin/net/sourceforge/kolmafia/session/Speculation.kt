package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.equipment.ModeableState
import net.sourceforge.kolmafia.maximizer.ReplaceableEffectMutex
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import net.sourceforge.kolmafia.modifiers.DerivedModifier
import net.sourceforge.kolmafia.modifiers.ModifierValues
import net.sourceforge.kolmafia.modifiers.StringModifier
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [net.sourceforge.kolmafia.Speculation] — hypothetical equipment/effect loadout
 * for Relay UseLink tooltips and `whatif`/`speculate` modifier prediction.
 */
class Speculation(
    baseState: CharacterState = CharacterState(),
    baseEffects: List<EffectData> = emptyList(),
    private val passiveSkillNames: Set<String> = emptySet(),
    private val preferences: Preferences? = null,
) {
    private var mindControl: Int = baseState.mindControlLevel
    private var equipment: MutableMap<EquipmentSlot, String> =
        baseState.equipment.toMutableMap()
    private var effects: MutableList<EffectData> = baseEffects.toMutableList()
    private var familiarName: String = baseState.familiarName
    private var enthronedName: String = baseState.enthronedFamiliarName
    private var bjornedName: String = baseState.bjornedFamiliarName
    private var custom: String? = null
    private var horsery: String? = preferences?.getString("_horsery").orEmpty().ifBlank { null }
    private var boomBox: String? = preferences?.getString("boomBoxSong").orEmpty().ifBlank { null }
    private var modeables: MutableMap<Modeable, String> =
        ModeableState.currentModes(preferences).toMutableMap()

    private val baseSnapshot = baseState
    private var calculated: ModifierValues = ModifierValues.EMPTY
    private var calculatedDerived: Map<DerivedModifier, Int> = emptyMap()
    private var didCalculate = false

    init {
        // Strip intrinsic effects granted by currently equipped items — they re-apply via mods.
        val strip = mutableSetOf<String>()
        for (name in equipment.values) {
            if (name.isBlank()) continue
            val intrinsic = ModifierDatabase.getStringModifier(name, StringModifier.INTRINSIC_EFFECT)
            if (intrinsic.isNotBlank()) strip += intrinsic.lowercase()
        }
        if (strip.isNotEmpty()) {
            effects.removeAll { it.name.lowercase() in strip }
        }
    }

    fun setMindControlLevel(level: Int) {
        mindControl = level
        didCalculate = false
    }

    fun equip(slot: EquipmentSlot, itemName: String) {
        equipment[slot] = itemName
        val itemId = ItemDatabase.getByName(itemName)?.id ?: 0
        if (slot == EquipmentSlot.WEAPON && itemId > 0 && EquipmentDatabase.getHands(itemId) > 1) {
            equipment[EquipmentSlot.OFFHAND] = ""
        }
        didCalculate = false
    }

    fun unequip(slot: EquipmentSlot) {
        equipment[slot] = ""
        didCalculate = false
    }

    fun addEffect(effect: EffectData) {
        effects = ReplaceableEffectMutex.applyEffectGain(effects, effect).toMutableList()
        didCalculate = false
    }

    fun removeEffect(effectId: Int) {
        effects.removeAll { it.id == effectId }
        didCalculate = false
    }

    fun setFamiliar(name: String) {
        familiarName = name
        didCalculate = false
    }

    fun setEnthroned(name: String) {
        enthronedName = name
        didCalculate = false
    }

    fun setBjorned(name: String) {
        bjornedName = name
        didCalculate = false
    }

    fun setCustom(value: String?) {
        custom = value
        didCalculate = false
    }

    fun setHorsery(value: String?) {
        horsery = value
        didCalculate = false
    }

    fun setBoomBox(value: String?) {
        boomBox = value
        didCalculate = false
    }

    fun setModeable(modeable: Modeable, value: String) {
        modeables[modeable] = value
        didCalculate = false
    }

    fun calculate(): CurrentModifiers {
        val state = baseSnapshot.copy(
            equipment = equipment.toMap(),
            familiarName = familiarName,
            enthronedFamiliarName = enthronedName,
            bjornedFamiliarName = bjornedName,
        )
        val mods = CurrentModifiers(
            state = state,
            activeEffects = effects.toList(),
            passiveSkillNames = passiveSkillNames,
            modeOverrides = modeables.toMap(),
            preferences = preferences,
            horseryOverride = horsery,
            boomBoxOverride = boomBox,
            mindControlOverride = mindControl,
            customModifierOverlay = custom,
        )
        calculated = mods.values
        calculatedDerived = mods.derived
        didCalculate = true
        return mods
    }

    fun getModifiers(): ModifierValues {
        if (!didCalculate) calculate()
        return calculated
    }

    fun getDerived(): Map<DerivedModifier, Int> {
        if (!didCalculate) calculate()
        return calculatedDerived
    }

    /**
     * Desktop [Speculation.parse] — `MCD` / `equip` / `unequip` / `familiar` / `up` / `uneffect` / `quiet`.
     * @return true when quiet (suppress HTML print)
     */
    fun parse(text: String): Boolean {
        var quiet = false
        for (raw in text.split(Regex("\\s*;\\s*"))) {
            val piece = raw.trim()
            if (piece.isEmpty()) continue
            val parts = piece.split(Regex("\\s+"), limit = 2)
            val cmd = parts[0].lowercase()
            val params = parts.getOrNull(1).orEmpty().trim()
            when (cmd) {
                "quiet" -> quiet = true
                "mcd" -> setMindControlLevel(params.toIntOrNull() ?: 0)
                "equip" -> {
                    val tokens = params.split(Regex("\\s+"), limit = 2)
                    val maybeSlot = EquipmentSlot.entries.find {
                        it.apiKey.equals(tokens[0], ignoreCase = true)
                    }
                    val itemQuery: String
                    val slot: EquipmentSlot?
                    if (maybeSlot != null && tokens.size == 2) {
                        slot = maybeSlot
                        itemQuery = tokens[1]
                    } else {
                        itemQuery = params
                        val itemId = ItemDatabase.getByName(itemQuery)?.id ?: 0
                        slot = chooseEquipmentSlot(itemId, equipment)
                    }
                    if (slot != null && itemQuery.isNotBlank()) {
                        val name = ItemDatabase.getByName(itemQuery)?.name ?: itemQuery
                        equip(slot, name)
                    }
                }
                "unequip" -> {
                    val slot = EquipmentSlot.entries.find {
                        it.apiKey.equals(params, ignoreCase = true)
                    }
                    if (slot != null) unequip(slot)
                }
                "familiar" -> if (params.isNotBlank()) setFamiliar(params)
                "enthrone" -> if (params.isNotBlank()) setEnthroned(params)
                "bjornify" -> if (params.isNotBlank()) setBjorned(params)
                "up" -> {
                    val effect = resolveEffect(params) ?: continue
                    addEffect(effect)
                }
                "uneffect", "shrug" -> {
                    val effect = resolveEffect(params) ?: continue
                    removeEffect(effect.id)
                }
                else -> {
                    val modeable = Modeable.entries.find {
                        it.name.equals(cmd, ignoreCase = true) ||
                            it.itemName.equals(cmd, ignoreCase = true)
                    }
                    if (modeable != null) setModeable(modeable, params)
                }
            }
        }
        return quiet
    }

    companion object {
        private val SPECULATION_CMDS = setOf(
            "mcd", "equip", "unequip", "familiar", "enthrone", "bjornify",
            "up", "uneffect", "shrug", "quiet",
        )

        /** True when [text] looks like desktop SpeculateCommand parameters (not Maximizer goals). */
        fun looksLikeSpeculationParams(text: String): Boolean {
            val first = text.trim().split(Regex("\\s+|;")).firstOrNull()?.lowercase().orEmpty()
            if (first in SPECULATION_CMDS) return true
            return Modeable.entries.any {
                it.name.equals(first, ignoreCase = true) ||
                    it.itemName.equals(first, ignoreCase = true)
            }
        }

        /** Desktop [EquipmentRequest.chooseEquipmentSlot]. */
        fun chooseEquipmentSlot(
            itemId: Int,
            equipment: Map<EquipmentSlot, String> = emptyMap(),
        ): EquipmentSlot? {
            val item = ItemDatabase.getById(itemId) ?: return null
            return when (item.primaryUse) {
                ItemPrimaryUse.HAT -> EquipmentSlot.HAT
                ItemPrimaryUse.WEAPON, ItemPrimaryUse.SIXGUN -> EquipmentSlot.WEAPON
                ItemPrimaryUse.OFFHAND -> EquipmentSlot.OFFHAND
                ItemPrimaryUse.CONTAINER -> EquipmentSlot.CONTAINER
                ItemPrimaryUse.SHIRT -> EquipmentSlot.SHIRT
                ItemPrimaryUse.PANTS -> EquipmentSlot.PANTS
                ItemPrimaryUse.ACCESSORY ->
                    listOf(EquipmentSlot.ACC1, EquipmentSlot.ACC2, EquipmentSlot.ACC3)
                        .firstOrNull { equipment[it].isNullOrBlank() }
                        ?: EquipmentSlot.ACC1
                ItemPrimaryUse.FAMILIAR -> EquipmentSlot.FAMILIAR
                else -> null
            }
        }

        fun fromLive(
            state: CharacterState,
            effects: List<EffectData>,
            passiveSkillNames: Set<String> = emptySet(),
            preferences: Preferences? = null,
        ): Speculation = Speculation(state, effects, passiveSkillNames, preferences)

        private fun resolveEffect(query: String): EffectData? {
            if (query.isBlank()) return null
            val byName = net.sourceforge.kolmafia.data.EffectDatabase.getByName(query)
            if (byName != null) {
                return EffectData(id = byName.id, name = byName.name, duration = 1)
            }
            val id = query.toIntOrNull() ?: return null
            val byId = net.sourceforge.kolmafia.data.EffectDatabase.getById(id) ?: return null
            return EffectData(id = byId.id, name = byId.name, duration = 1)
        }
    }
}
