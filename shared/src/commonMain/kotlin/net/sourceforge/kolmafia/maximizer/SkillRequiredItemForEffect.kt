package net.sourceforge.kolmafia.maximizer

/** Desktop [net.sourceforge.kolmafia.request.UseSkillRequest.requiredItemForSkillEffect]. */
object SkillRequiredItemForEffect {

    private data class ReplaceEffect(
        val skillId: Int,
        val itemId: Int,
        val baseEffectId: Int,
        val newEffectId: Int,
    )

    private data class AdditionalEffect(
        val skillId: Int,
        val itemId: Int,
        val newEffectId: Int,
    )

    private const val APRIL_SHOWER_THOUGHTS_SHIELD = 11884
    private const val VELOUR_VOULGE = 10114
    private const val VELOUR_VISCOMETER = 10117
    private const val VELOUR_VAQUEROS = 10120
    private const val LEGENDARY_PASTA_WAND = 12223

    private val replaceEffects = listOf(
        ReplaceEffect(1020, VELOUR_VOULGE, 222, 2555),
        ReplaceEffect(4019, VELOUR_VISCOMETER, 224, 2553),
        ReplaceEffect(6018, VELOUR_VAQUEROS, 225, 2554),
        ReplaceEffect(2009, APRIL_SHOWER_THOUGHTS_SHIELD, 50, 2989),
        ReplaceEffect(3027, LEGENDARY_PASTA_WAND, 1445, 3095),
        ReplaceEffect(3029, LEGENDARY_PASTA_WAND, 1446, 3096),
        ReplaceEffect(3031, LEGENDARY_PASTA_WAND, 1447, 3097),
        ReplaceEffect(3033, LEGENDARY_PASTA_WAND, 1448, 3098),
        ReplaceEffect(3035, LEGENDARY_PASTA_WAND, 1449, 3099),
        ReplaceEffect(3037, LEGENDARY_PASTA_WAND, 1450, 3100),
        ReplaceEffect(3039, LEGENDARY_PASTA_WAND, 1451, 3101),
    )

    private val additionalEffects = listOf(
        AdditionalEffect(1000, APRIL_SHOWER_THOUGHTS_SHIELD, 2983),
        AdditionalEffect(2000, APRIL_SHOWER_THOUGHTS_SHIELD, 2984),
        AdditionalEffect(3000, APRIL_SHOWER_THOUGHTS_SHIELD, 2985),
        AdditionalEffect(4000, APRIL_SHOWER_THOUGHTS_SHIELD, 2986),
        AdditionalEffect(5000, APRIL_SHOWER_THOUGHTS_SHIELD, 2987),
        AdditionalEffect(6000, APRIL_SHOWER_THOUGHTS_SHIELD, 2988),
    )

    fun requiredItem(skillId: Int, effectId: Int): Int {
        for (replace in replaceEffects) {
            if (skillId != replace.skillId) continue
            if (effectId == replace.baseEffectId) return -1
            if (effectId == replace.newEffectId) return replace.itemId
        }
        for (add in additionalEffects) {
            if (skillId == add.skillId && effectId == add.newEffectId) {
                return add.itemId
            }
        }
        return -1
    }
}
