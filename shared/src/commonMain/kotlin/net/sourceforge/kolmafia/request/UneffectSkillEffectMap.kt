package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.data.EffectDatabase

/** Desktop [UneffectRequest.EFFECT_SKILL] from statuseffects `cast 1` default actions. */
object UneffectSkillEffectMap {
    private var effectToSkill: Map<String, String> = emptyMap()
    private var skillToEffectMap: Map<String, String> = emptyMap()

    fun rebuild() {
        val forward = linkedMapOf<String, String>()
        for (effect in EffectDatabase.all()) {
            val actions = effect.actions ?: continue
            if (!actions.startsWith("cast 1")) continue
            var skillName = actions.substring(7)
            if (skillName.contains('|')) {
                skillName = skillName.substring(0, skillName.indexOf('|'))
            }
            if (skillName.contains(" ^ ")) {
                skillName = skillName.substring(0, skillName.indexOf(" ^ "))
            }
            forward[effect.name] = skillName
        }
        effectToSkill = forward
        val reverse = linkedMapOf<String, String>()
        for ((effectName, skillName) in forward) {
            reverse.putIfAbsent(skillName.lowercase(), effectName)
        }
        skillToEffectMap = reverse
    }

    fun effectToSkill(effectName: String): String? {
        ensureBuilt()
        return effectToSkill.entries.firstOrNull { it.key.equals(effectName, ignoreCase = true) }?.value
    }

    fun skillToEffect(skillName: String): String? {
        ensureBuilt()
        return skillToEffectMap[skillName.lowercase()]
    }

    internal fun resetForTest() {
        effectToSkill = emptyMap()
        skillToEffectMap = emptyMap()
    }

    private fun ensureBuilt() {
        if (effectToSkill.isNotEmpty() || EffectDatabase.all().isEmpty()) return
        rebuild()
    }
}
