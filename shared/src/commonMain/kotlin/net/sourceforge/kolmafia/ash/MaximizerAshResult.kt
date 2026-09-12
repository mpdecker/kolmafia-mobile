package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.maximizer.MaximizerBoost
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap

/**
 * Desktop RuntimeLibrary.maximizerResult / maximizerResultFull record shapes for ASH maximize
 * 5-arg overloads (Phases 6531–6550 Track C).
 *
 * Field names match desktop RecordType (`display`/`command`/`score`/…); boost payload maps from
 * MaximizerBoost text/cmd/delta locals.
 */
internal val MAXIMIZER_RESULT_REC = RecordType(
    "{string display; string command; float score; effect effect; item item; skill skill;}",
    listOf(
        RecordField("display", AshType.STRING, 0),
        RecordField("command", AshType.STRING, 1),
        RecordField("score", AshType.FLOAT, 2),
        RecordField("effect", AshType.EFFECT, 3),
        RecordField("item", AshType.ITEM, 4),
        RecordField("skill", AshType.SKILL, 5),
    ),
)

internal val MAXIMIZER_RESULT_FULL_REC = RecordType(
    "{string display; string command; float score; effect effect; item item; skill skill; string afterdisplay;}",
    listOf(
        RecordField("display", AshType.STRING, 0),
        RecordField("command", AshType.STRING, 1),
        RecordField("score", AshType.FLOAT, 2),
        RecordField("effect", AshType.EFFECT, 3),
        RecordField("item", AshType.ITEM, 4),
        RecordField("skill", AshType.SKILL, 5),
        RecordField("afterdisplay", AshType.STRING, 6),
    ),
)

internal val MAXIMIZER_RESULT_ARRAY = AggregateType(AshType.INT, MAXIMIZER_RESULT_REC)
internal val MAXIMIZER_RESULT_FULL_ARRAY = AggregateType(AshType.INT, MAXIMIZER_RESULT_FULL_REC)

/** Desktop RuntimeLibrary.maximize 5-arg: skip leading equipment boosts when showEquipment is false. */
internal fun filterMaximizerBoostsForAsh(
    boosts: List<MaximizerBoost>,
    showEquipment: Boolean,
): List<MaximizerBoost> {
    if (showEquipment) return boosts
    var lastEquipIndex = 0
    for (boost in boosts) {
        if (!boost.isEquipment) break
        lastEquipIndex++
    }
    return boosts.drop(lastEquipIndex)
}

internal fun maximizerBoostsToAshRecords(
    boosts: List<MaximizerBoost>,
    full: Boolean,
): AggregateValue {
    val type = if (full) MAXIMIZER_RESULT_FULL_ARRAY else MAXIMIZER_RESULT_ARRAY
    val recordType = if (full) MAXIMIZER_RESULT_FULL_REC else MAXIMIZER_RESULT_REC
    val result = AggregateValue(type)
    boosts.forEachIndexed { index, boost ->
        result[AshValue.of(index.toLong())] = maximizerBoostToRecord(boost, recordType, full)
    }
    return result
}

internal fun maximizerBoostToRecord(
    boost: MaximizerBoost,
    recordType: RecordType,
    full: Boolean,
): RecordValue {
    var text = boost.text
    var afterText = ""
    val cutIndex = text.indexOf(" (")
    if (cutIndex != -1) {
        afterText = text.substring(cutIndex + 1)
        text = text.substring(0, cutIndex)
    }

    val effectName = if (!boost.isEquipment) {
        boost.effectName?.takeIf { it.isNotBlank() }
    } else {
        null
    }
    val itemName = when {
        boost.itemName.isNotBlank() -> boost.itemName
        boost.itemId > 0 -> ItemDatabase.getById(boost.itemId)?.name
        else -> null
    }
    val skillName = if (boost.cmd.startsWith("cast", ignoreCase = true) && effectName != null) {
        UneffectSkillEffectMap.effectToSkill(effectName)
    } else {
        null
    }

    val rec = RecordValue(recordType)
    rec.setField(0, AshValue.of(text))
    rec.setField(1, AshValue.of(boost.cmd))
    rec.setField(2, AshValue.of(boost.delta))
    rec.setField(3, if (effectName != null) AshValue.effect(effectName) else AshType.EFFECT.defaultValue())
    rec.setField(4, if (itemName != null) AshValue.item(itemName) else AshType.ITEM.defaultValue())
    rec.setField(5, if (skillName != null) AshValue.skill(skillName) else AshType.SKILL.defaultValue())
    if (full) {
        rec.setField(6, AshValue.of(afterText))
    }
    return rec
}
