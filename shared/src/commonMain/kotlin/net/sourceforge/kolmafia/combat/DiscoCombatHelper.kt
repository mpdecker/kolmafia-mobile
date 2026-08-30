package net.sourceforge.kolmafia.combat

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.NemesisManager

/**
 * Desktop [DiscoCombatHelper] core (Phases 1551–1610) — rave combo prefs, fight learning,
 * and CCS/Macrofier skill expansion. Relay decorate is intentionally excluded.
 */
object DiscoCombatHelper {

    const val UNKNOWN = -1
    const val BREAK_IT_ON_DOWN = 0
    const val POP_AND_LOCK_IT = 1
    const val RUN_LIKE_THE_WIND = 2

    const val FIRST_RAVE_SKILL = 0
    const val LAST_RAVE_SKILL = 2
    const val NUM_SKILLS = 3

    val SKILLS = arrayOf("Break It On Down", "Pop and Lock It", "Run Like the Wind")
    val SKILL_ID = intArrayOf(50, 51, 52)

    const val RAVE_CONCENTRATION = 0
    const val RAVE_NIRVANA = 1
    const val RAVE_KNOCKOUT = 2
    const val RAVE_BLEEDING = 3
    const val RAVE_STEAL = 4
    const val RAVE_SUBSTATS = 5
    const val RANDOM_RAVE = 6

    const val FIRST_RAVE_COMBO = 0
    const val NUM_COMBOS = 7

    val COMBOS = arrayOf(
        arrayOf("Rave Concentration", "Item Drop +30"),
        arrayOf("Rave Nirvana", "Meat Drop +50"),
        arrayOf("Rave Knockout", "Multi-round stun+damage"),
        arrayOf("Rave Bleeding", "Recurring damage"),
        arrayOf("Rave Steal", "Steal item"),
        arrayOf("Rave Substats", "2-4 substats"),
        arrayOf("Random Rave", "Learn a new combo!"),
    )

    private val VOLCANO_RAVERS = setOf(
        "Breakdancing Raver",
        "Pop-and-Lock Raver",
        "Running Man",
    )

    var canCombo: Boolean = false
        private set

    val knownSkill = BooleanArray(NUM_SKILLS)
    val knownCombo = BooleanArray(NUM_COMBOS)

    /** [combo][slot][alt] — each slot holds one primary skill index (desktop multi-alt unused). */
    private val comboSkills: Array<Array<IntArray>> = Array(NUM_COMBOS) {
        Array(3) { intArrayOf(UNKNOWN) }
    }

    private var counter = 0
    private val sequence = IntArray(3)

    private var preferences: Preferences? = null
    private var hasSkillByName: (String) -> Boolean = { false }
    private var lastMonsterName: () -> String = { MonsterStatusTracker.getLastMonsterName() }

    fun resetForTest() {
        canCombo = false
        knownSkill.fill(false)
        knownCombo.fill(false)
        for (c in 0 until NUM_COMBOS) {
            for (s in 0 until 3) comboSkills[c][s][0] = UNKNOWN
        }
        counter = 0
        sequence.fill(0)
        preferences = null
        hasSkillByName = { false }
        lastMonsterName = { MonsterStatusTracker.getLastMonsterName() }
    }

    fun initialize(
        isDiscoBandit: Boolean,
        preferences: Preferences?,
        hasSkill: (String) -> Boolean = { name ->
            val idx = skillNameToSkill(name)
            if (idx < 0) false
            else preferences?.getInt("skillLevel${SKILL_ID[idx]}", 0)?.let { it > 0 } == true
        },
        monsterNameProvider: () -> String = { MonsterStatusTracker.getLastMonsterName() },
    ) {
        this.preferences = preferences
        this.hasSkillByName = hasSkill
        this.lastMonsterName = monsterNameProvider
        canCombo = isDiscoBandit
        if (!canCombo) return

        for (i in 0 until NUM_SKILLS) {
            knownSkill[i] = hasSkill(SKILLS[i])
        }
        for (i in 0 until NUM_COMBOS) {
            checkCombo(i)
        }
        counter = 0
        sequence.fill(0)
    }

    fun canRaveSteal(monsterName: String = lastMonsterName()): Boolean {
        val prefs = preferences
        if (prefs == null || prefs.getInt("_raveStealCount", 0) < 30) return true
        return monsterName in VOLCANO_RAVERS
    }

    fun disambiguateCombo(name: String): String? {
        val needle = name.trim().lowercase()
        if (needle.isEmpty()) return null
        for (i in 0 until NUM_COMBOS) {
            if (COMBOS[i][0].lowercase().contains(needle)) return COMBOS[i][0]
        }
        return null
    }

    fun getCombo(name: String): IntArray? {
        val needle = name.trim().lowercase()
        for (i in 0 until NUM_COMBOS) {
            if (COMBOS[i][0].lowercase().contains(needle)) return getCombo(i)
        }
        return null
    }

    fun getCombo(combo: Int): IntArray? {
        if (!canCombo || combo !in 0 until NUM_COMBOS || !knownCombo[combo]) return null
        val data = comboSkills[combo]
        val rv = IntArray(data.size)
        for (i in data.indices) {
            val alts = data[i]
            var picked = 0
            for (skill in alts) {
                if (skill in 0 until NUM_SKILLS && knownSkill[skill]) {
                    picked = SKILL_ID[skill]
                    break
                }
            }
            rv[i] = picked
        }
        return rv
    }

    fun learnSkill(name: String) {
        if (!canCombo) return
        var discoSkill = false
        for (i in 0 until NUM_SKILLS) {
            if (SKILLS[i].equals(name, ignoreCase = true)) {
                discoSkill = true
                knownSkill[i] = true
                break
            }
        }
        if (!discoSkill) return
        for (i in 0 until NUM_COMBOS) checkCombo(i)
    }

    /**
     * Desktop [DiscoCombatHelper.parseFightRound] — [action] like `skill50` / `skill 50` / null.
     * [responseText] is the fight round HTML fragment (full page is fine).
     */
    fun parseFightRound(action: String?, responseText: String) {
        if (!canCombo || responseText.isBlank()) return

        if (counter == 3) {
            if (responseText.contains("seems to be temporarily unconscious")) {
                learnRaveCombo(RAVE_KNOCKOUT)
            }
            if (responseText.contains("bleeds from various wounds you've inflicted")) {
                learnRaveCombo(RAVE_BLEEDING)
            }
        }

        if (action == null || !action.startsWith("skill", ignoreCase = true)) {
            counter = 0
            return
        }
        val idStr = action.removePrefix("skill").trim()
        val skill = skillIdToSkill(idStr.toIntOrNull() ?: -1)
        if (skill < 0) {
            counter = 0
            return
        }

        var index = counter
        if (index == 3) {
            sequence[0] = sequence[1]
            sequence[1] = sequence[2]
            index = 2
        }
        sequence[index++] = skill
        counter = index

        var combo = -1
        for (i in 0 until NUM_COMBOS) {
            if (counter <= 1) break
            if (!knownCombo[i] || i == RANDOM_RAVE) continue
            val data = comboSkills[i]
            if (counter == data.size && checkSequence(data, 0)) {
                combo = i
                counter = 0
                break
            }
            if (counter == 3 && data.size == 2 && checkSequence(data, 1)) {
                combo = i
                counter = 0
                break
            }
        }

        if (combo == RAVE_STEAL) {
            val encounter = lastMonsterName()
            if (encounter !in VOLCANO_RAVERS) {
                val prefs = preferences
                when {
                    responseText.contains("same old song and dance") ->
                        prefs?.setInt("_raveStealCount", 30)
                    responseText.contains("You acquire an item") ->
                        prefs?.setInt(
                            "_raveStealCount",
                            prefs.getInt("_raveStealCount", 0) + 1,
                        )
                }
            }
        }

        if (counter == 3) {
            when {
                responseText.contains("Your savage beatdown") -> learnRaveCombo(RAVE_STEAL)
                responseText.contains("extra dance practice") -> learnRaveCombo(RAVE_SUBSTATS)
                responseText.contains("extra-focused and in the zone") ->
                    learnRaveCombo(RAVE_CONCENTRATION)
                responseText.contains("feeling particularly groovy") ->
                    learnRaveCombo(RAVE_NIRVANA)
            }
        }
    }

    /** Desktop [NemesisManager.ensureUpdatedNemesisStatus] raveCombo clear slice. */
    fun ensureUpdatedNemesisStatus(preferences: Preferences, ascensions: Int): Boolean {
        val alreadyCurrent = preferences.getInt("lastNemesisReset", -1) == ascensions
        NemesisManager.resetForAscension(preferences, ascensions)
        if (alreadyCurrent) return false
        if (canCombo) {
            for (i in 0 until NUM_COMBOS) checkCombo(i)
        }
        return true
    }

    internal fun learnRaveComboForTest(combo: Int, skill1: Int, skill2: Int, skill3: Int) {
        learnRaveCombo(combo, skill1, skill2, skill3)
        checkCombo(RANDOM_RAVE)
    }

    private fun learnRaveCombo(combo: Int) {
        if (counter != 3) return
        val skill1 = sequence[0]
        val skill2 = sequence[1]
        val skill3 = sequence[2]
        if (skill1 == skill2 || skill1 == skill3 || skill2 == skill3) return
        if (skill1 !in FIRST_RAVE_SKILL..LAST_RAVE_SKILL ||
            skill2 !in FIRST_RAVE_SKILL..LAST_RAVE_SKILL ||
            skill3 !in FIRST_RAVE_SKILL..LAST_RAVE_SKILL
        ) {
            return
        }
        counter = 0
        if (knownCombo[combo]) return
        learnRaveCombo(combo, skill1, skill2, skill3)
        checkCombo(RANDOM_RAVE)
    }

    private fun learnRaveCombo(combo: Int, skill1: Int, skill2: Int, skill3: Int) {
        knownCombo[combo] = true
        val setting = "raveCombo${combo - FIRST_RAVE_COMBO + 1}"
        val value = "${SKILLS[skill1]},${SKILLS[skill2]},${SKILLS[skill3]}"
        preferences?.setString(setting, value)
        comboSkills[combo][0][0] = skill1
        comboSkills[combo][1][0] = skill2
        comboSkills[combo][2][0] = skill3
    }

    private fun checkCombo(combo: Int) {
        val data = comboSkills[combo]
        if (combo == RANDOM_RAVE) {
            var found = 0
            outer@ for (sel in 0 until 27) {
                val s1 = sel % 3 + FIRST_RAVE_SKILL
                val s2 = (sel / 3) % 3 + FIRST_RAVE_SKILL
                val s3 = (sel / 9) % 3 + FIRST_RAVE_SKILL
                if (s1 == s2 || s1 == s3 || s2 == s3) continue
                for (test in FIRST_RAVE_COMBO until RANDOM_RAVE) {
                    val td = comboSkills[test]
                    if (s1 == td[0][0] && s2 == td[1][0] && s3 == td[2][0]) continue@outer
                }
                if (found == 1) {
                    found = 2
                    break
                }
                data[0][0] = s1
                data[1][0] = s2
                data[2][0] = s3
                found = 1
            }
            when (found) {
                1 -> {
                    for (test in FIRST_RAVE_COMBO until RANDOM_RAVE) {
                        if (!knownCombo[test]) {
                            learnRaveCombo(test, data[0][0], data[1][0], data[2][0])
                            break
                        }
                    }
                    knownCombo[combo] = false
                    return
                }
                0 -> {
                    knownCombo[combo] = false
                    return
                }
            }
            // fall through: ≥2 unknown — Random Rave available if skills known
        } else if (combo >= FIRST_RAVE_COMBO) {
            val setting = "raveCombo${combo - FIRST_RAVE_COMBO + 1}"
            val seq = preferences?.getString(setting, "").orEmpty()
            val skills = seq.split(",")
            if (skills.size == 3) {
                for (i in skills.indices) {
                    val skill = skillNameToSkill(skills[i].trim())
                    if (skill < FIRST_RAVE_SKILL || skill > LAST_RAVE_SKILL) {
                        knownCombo[combo] = false
                        return
                    }
                    data[i][0] = skill
                }
                knownCombo[combo] = true
                return
            }
            knownCombo[combo] = false
            return
        }

        for (i in data.indices) {
            val alts = data[i]
            var known = false
            for (skill in alts) {
                if (skill != UNKNOWN && skill in knownSkill.indices && knownSkill[skill]) {
                    known = true
                    break
                }
            }
            if (!known) {
                knownCombo[combo] = false
                return
            }
        }
        knownCombo[combo] = true
    }

    private fun checkSequence(data: Array<IntArray>, offset: Int): Boolean {
        for (i in data.indices) {
            val skill = sequence[i + offset]
            if (data[i].none { it == skill }) return false
        }
        return true
    }

    private fun skillIdToSkill(skillId: Int): Int {
        for (i in 0 until NUM_SKILLS) {
            if (skillId == SKILL_ID[i]) return i
        }
        return -1
    }

    private fun skillNameToSkill(name: String): Int {
        for (i in 0 until NUM_SKILLS) {
            if (SKILLS[i].equals(name, ignoreCase = true)) return i
        }
        return -1
    }
}
