package net.sourceforge.kolmafia.mood

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionProxy
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap
import net.sourceforge.kolmafia.skill.SkillState

/** Desktop [MoodManager.minimalSet] / [MoodManager.maximalSet] mood autofill constants. */
object MoodAutofill {
    val skipSkillIds: Set<Int> = setOf(
        1019, 6016, 11019, // combat rate increasers
        3024, 6014, 6028, 11023, // out-of-battle buffs
    )

    val hardcoreThiefBuffs: Array<String> = arrayOf(
        "Fat Leon's Phat Loot Lyric",
        "The Moxious Madrigal",
        "Aloysius' Antiphon of Aptitude",
        "The Sonata of Sneakiness",
        "The Psalm of Pointiness",
        "Ur-Kel's Aria of Annoyance",
    )

    val softcoreThiefBuffs: Array<String> = arrayOf(
        "Fat Leon's Phat Loot Lyric",
        "Aloysius' Antiphon of Aptitude",
        "Ur-Kel's Aria of Annoyance",
        "The Sonata of Sneakiness",
        "Jackasses' Symphony of Destruction",
        "Cletus's Canticle of Celerity",
    )

    val rankedBorisSongs: Array<String> = arrayOf(
        "Song of Fortune",
        "Song of Accompaniment",
        "Song of Solitude",
        "Song of Cockiness",
    )
}

/** Desktop mood autofill helpers operating on [MoodManager]. */
fun MoodManager.canAutofill(): Boolean {
    val mood = activeMood ?: return false
    return !mood.name.equals("apathetic", ignoreCase = true)
}

fun MoodManager.addActiveLoseEffectTrigger(effectName: String, action: String): Boolean {
    if (action.isBlank() || !canAutofill()) return false
    return addActiveRemovalTrigger("lose_effect", effectName, action) != null
}

fun MoodManager.minimalSet(effectState: EffectState) {
    if (!canAutofill()) return
    for (effect in effectState.effects) {
        val action = getDefaultAction("lose_effect", effect.name)
        if (action.isNotEmpty()) {
            addActiveLoseEffectTrigger(effect.name, action)
        }
    }
}

fun MoodManager.pickSkills(
    skills: List<String>,
    limit: Int,
    rankedBuffs: Array<String>,
    skillState: SkillState,
) {
    if (skills.isEmpty()) return
    if (skills.size <= limit) {
        for (skillName in skills) {
            val effectName = UneffectSkillEffectMap.skillToEffect(skillName) ?: continue
            addActiveLoseEffectTrigger(effectName, "cast $skillName")
        }
        return
    }
    var foundSkillCount = 0
    for (ranked in rankedBuffs) {
        if (foundSkillCount >= limit) break
        if (!skillState.skills.any { it.name.equals(ranked, ignoreCase = true) }) continue
        val effectName = UneffectSkillEffectMap.skillToEffect(ranked) ?: continue
        addActiveLoseEffectTrigger(effectName, "cast $ranked")
        foundSkillCount++
    }
}

fun MoodManager.maximalSet(
    effectState: EffectState,
    skillState: SkillState,
    charState: CharacterState,
) {
    if (!canAutofill()) return

    val thiefSkills = mutableListOf<String>()
    val borisSongs = mutableListOf<String>()

    for (skill in skillState.skills) {
        val skillId = skill.id
        if (skillId < 1000) continue
        if (skillId in MoodAutofill.skipSkillIds) continue

        if (SkillDefinitionProxy.isAccordionThiefSong(skillId)) {
            thiefSkills.add(skill.name)
            continue
        }

        if (skillId in 11000..11999 && SkillDefinitionProxy.isSong(skillId)) {
            borisSongs.add(skill.name)
            continue
        }

        val effectName = UneffectSkillEffectMap.skillToEffect(skill.name) ?: continue
        if (EffectDatabase.getByName(effectName) == null) continue
        val action = getDefaultAction("lose_effect", effectName)
        if (action.isNotEmpty()) {
            addActiveLoseEffectTrigger(effectName, action)
        }
    }

    if (borisSongs.isNotEmpty()) {
        pickSkills(borisSongs, 1, MoodAutofill.rankedBorisSongs, skillState)
    }

    if (thiefSkills.isNotEmpty()) {
        val rankedBuffs = if (charState.isHardcore) {
            MoodAutofill.hardcoreThiefBuffs
        } else {
            MoodAutofill.softcoreThiefBuffs
        }
        pickSkills(thiefSkills, charState.atSongLimit, rankedBuffs, skillState)
    }

    minimalSet(effectState)
}
