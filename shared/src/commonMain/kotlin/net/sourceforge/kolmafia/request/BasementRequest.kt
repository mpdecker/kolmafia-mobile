package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop BasementRequest — level sync + test classification / action selection
 * (Phases 2301–2315 level; Phases 2661–2675 autoSwitch engine depth).
 */
enum class BasementTestType {
    NONE,
    MONSTER,
    REWARD,
    ELEMENT,
    MUSCLE,
    MYSTICALITY,
    MOXIE,
    MPDRAIN,
    HPDRAIN,
}

object BasementSync {
    private val LEVEL = Regex("""Level ([\d,]+)""", RegexOption.IGNORE_CASE)

    @Volatile
    var basementLevel: Int = 0

    @Volatile
    var basementTest: BasementTestType = BasementTestType.NONE

    @Volatile
    var basementTestString: String = ""

    @Volatile
    var basementMonster: String = ""

    @Volatile
    var basementErrorMessage: String? = null

    @Volatile
    var element1: String = ""

    @Volatile
    var element2: String = ""

    @Volatile
    var lastResponseText: String = ""

    fun parseLevel(html: String): Int? =
        LEVEL.find(html)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()

    fun getBasementAction(text: String, isMuscleClass: Boolean, isMystClass: Boolean, isMoxieClass: Boolean): String =
        when {
            text.contains("Got Silk?") -> if (isMoxieClass) "1" else "2"
            text.contains("Save the Dolls") -> if (isMystClass) "1" else "2"
            text.contains("Take the Red Pill") -> if (isMuscleClass) "1" else "2"
            else -> "1"
        }

    fun getBasementLevelName(): String =
        "Fernswarthy's Basement (Level $basementLevel)"

    fun getBasementLevelSummary(): String = when (basementTest) {
        BasementTestType.NONE, BasementTestType.MONSTER -> basementTestString
        BasementTestType.REWARD -> basementTestString
        BasementTestType.ELEMENT -> "$basementTestString Test: $element1/$element2"
        BasementTestType.MUSCLE -> "Muscle Test: $basementTestString"
        BasementTestType.MYSTICALITY -> "Mysticality Test: $basementTestString"
        BasementTestType.MOXIE -> "Moxie Test: $basementTestString"
        BasementTestType.MPDRAIN -> "MP Drain: $basementTestString"
        BasementTestType.HPDRAIN -> "HP Drain: $basementTestString"
    }

    fun isElementalImmunity(name: String): Boolean =
        name in ELEMENT_FORMS

    fun checkBasement(
        html: String,
        preferences: Preferences? = null,
        autoSwitch: Boolean = false,
        muscle: Int = 0,
        mysticality: Int = 0,
        moxie: Int = 0,
        maxHp: Int = 0,
        maxMp: Int = 0,
    ): Int {
        lastResponseText = html
        basementErrorMessage = null
        basementMonster = ""
        basementTestString = ""
        element1 = ""
        element2 = ""
        basementTest = BasementTestType.NONE

        val level = parseLevel(html) ?: return basementLevel
        basementLevel = level
        preferences?.setInt("basementLevel", level)
        if (autoSwitch) {
            preferences?.setBoolean("_basementAutoChecked", true)
        }

        when {
            checkForReward(html) -> basementTest = BasementTestType.REWARD
            checkForElementalTest(html) -> basementTest = BasementTestType.ELEMENT
            checkForStatTest(html, autoSwitch, muscle, mysticality, moxie) -> Unit
            checkForDrainTest(html, autoSwitch, maxHp, maxMp) -> Unit
            checkForMonster(html) -> basementTest = BasementTestType.MONSTER
        }
        return level
    }

    private fun checkForReward(responseText: String): Boolean {
        val reward = when {
            responseText.contains("De Los Dioses") -> "Encounter: De Los Dioses"
            responseText.contains("The Dusk Zone") -> "Encounter: The Dusk Zone"
            responseText.contains("Giggity Bobbity Boo!") -> "Encounter: Giggity Bobbity Boo!"
            responseText.contains("No Good Deed") -> "Encounter: No Good Deed"
            responseText.contains(">Fernswarthy's Basement, Level 500</b>") ->
                "Encounter: Fernswarthy's Basement, Level 500"
            responseText.contains("Got Silk?") -> "Encounter: Got Silk?/Leather is Betther"
            responseText.contains("Save the Dolls") -> "Encounter: Save the Dolls/Save the Cardboard"
            responseText.contains("Take the Red Pill") -> "Encounter: Take the Red Pill/Take the Blue Pill"
            else -> null
        } ?: return false
        basementTestString = reward
        return true
    }

    private fun checkForElementalTest(responseText: String): Boolean {
        val pair = when {
            responseText.contains("<b>Peace, Bra!</b>") -> "stench" to "sleaze"
            responseText.contains("<b>Singled Out</b>") -> "cold" to "sleaze"
            responseText.contains("<b>Still Better than Pistachio</b>") -> "stench" to "hot"
            responseText.contains("<b>Unholy Writes</b>") -> "hot" to "spooky"
            responseText.contains("<b>The Unthawed</b>") -> "cold" to "spooky"
            responseText.contains("<b>A Burger Christmas</b>") -> "hot" to "sleaze"
            responseText.contains("<b>Collapse of the Huge</b>") -> "cold" to "stench"
            responseText.contains("<b>Great Balls of Odor</b>") -> "stench" to "spooky"
            responseText.contains("<b>Home, Home in the Range</b>") -> "hot" to "cold"
            responseText.contains("<b>The Horror... The Horror...</b>") -> "spooky" to "sleaze"
            else -> null
        } ?: return false
        element1 = pair.first
        element2 = pair.second
        basementTestString = "Elemental"
        return true
    }

    private fun checkForStatTest(
        responseText: String,
        autoSwitch: Boolean,
        muscle: Int,
        mysticality: Int,
        moxie: Int,
    ): Boolean {
        val requirement = (basementLevel * basementLevel).toDouble()
        when {
            responseText.contains("Lift 'em") ||
                responseText.contains("Push it Real Good") ||
                responseText.contains("Ring that Bell") -> {
                basementTest = BasementTestType.MUSCLE
                basementTestString = requirement.toLong().toString()
                if (muscle < requirement) {
                    basementErrorMessage = "You must have at least ${requirement.toLong()} muscle."
                }
                return true
            }
            responseText.contains("Gathering:  The Magic") ||
                responseText.contains("Mop the Floor") ||
                responseText.contains("'doo") -> {
                basementTest = BasementTestType.MYSTICALITY
                basementTestString = requirement.toLong().toString()
                if (mysticality < requirement) {
                    basementErrorMessage = "You must have at least ${requirement.toLong()} mysticality."
                }
                return true
            }
            responseText.contains("Don't Wake the Baby") ||
                responseText.contains("Grab a cue") ||
                responseText.contains("Smooth Moves") -> {
                basementTest = BasementTestType.MOXIE
                basementTestString = requirement.toLong().toString()
                if (moxie < requirement) {
                    basementErrorMessage = "You must have at least ${requirement.toLong()} moxie."
                }
                return true
            }
        }
        return false
    }

    private fun checkForDrainTest(
        responseText: String,
        autoSwitch: Boolean,
        maxHp: Int,
        maxMp: Int,
    ): Boolean {
        when {
            responseText.contains("Down the Hatch") ||
                responseText.contains("Drink it Up") ||
                responseText.contains("Sippin'") -> {
                basementTest = BasementTestType.MPDRAIN
                val need = basementLevel * 10.0
                basementTestString = need.toLong().toString()
                if (maxMp < need) {
                    basementErrorMessage = "Insufficient MP to continue."
                }
                return true
            }
            responseText.contains("A Rolling Stone") ||
                responseText.contains("It's Just a Flesh Wound") ||
                responseText.contains("The Undead Don't Sleep") -> {
                basementTest = BasementTestType.HPDRAIN
                val need = basementLevel * 10.0
                basementTestString = need.toLong().toString()
                if (maxHp < need) {
                    basementErrorMessage = "Insufficient health to continue."
                }
                return true
            }
        }
        return false
    }

    private fun checkForMonster(responseText: String): Boolean {
        val monster = when {
            responseText.contains("Don't Fear the Ear") -> "Beast with X Ears"
            responseText.contains("Commence to Pokin") -> "Beast with X Eyes"
            responseText.contains("Stone Golem") -> "X Stone Golem"
            responseText.contains("Hydra") -> "X-headed Hydra"
            responseText.contains("Toast that Ghost") -> "Ghost of Fernswarthy"
            responseText.contains("Bottles of Beer on a Golem") -> "Bottles of Beer on a Golem"
            responseText.contains("Collapse") -> "Collapse"
            responseText.contains("You Don't Mess Around with Gym") -> "You Don't Mess Around with Gym"
            else -> null
        } ?: return false
        basementMonster = monster
        val level = 2.0 * Math.pow(basementLevel.toDouble(), 1.4)
        basementTestString = "Monster: Attack/Defense = ${level.toLong()}"
        return true
    }

    fun resetForTest() {
        basementLevel = 0
        basementTest = BasementTestType.NONE
        basementTestString = ""
        basementMonster = ""
        basementErrorMessage = null
        element1 = ""
        element2 = ""
        lastResponseText = ""
    }

    val ELEMENT_PHIALS = listOf(
        "phial of hotness",
        "phial of coldness",
        "phial of spookiness",
        "phial of stench",
        "phial of sleaziness",
    )

    val ELEMENT_FORMS = listOf(
        "Hotform",
        "Coldform",
        "Spookyform",
        "Stenchform",
        "Sleazeform",
    )
}

open class BasementRequest(private val client: HttpClient) {
    open suspend fun visit(preferences: Preferences? = null): Result<Int> = try {
        val response = client.get("$KOL_BASE_URL/basement.php")
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            val html = response.bodyAsText()
            Result.success(BasementSync.checkBasement(html, preferences))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Desktop BasementRequest.run() — probe, classify, post action, verify advance.
     */
    open suspend fun runLevel(
        preferences: Preferences? = null,
        isMuscleClass: Boolean = false,
        isMystClass: Boolean = false,
        isMoxieClass: Boolean = false,
    ): Result<Int> {
        return try {
            val probe = client.get("$KOL_BASE_URL/basement.php")
            if (!probe.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${probe.status.value}"))
            }
            val probeHtml = probe.bodyAsText()
            val before = BasementSync.checkBasement(probeHtml, preferences, autoSwitch = true)
            if (BasementSync.basementErrorMessage != null) {
                return Result.failure(Exception(BasementSync.basementErrorMessage))
            }
            val action = BasementSync.getBasementAction(
                probeHtml, isMuscleClass, isMystClass, isMoxieClass,
            )
            val post = client.get("$KOL_BASE_URL/basement.php") {
                parameter("action", action)
            }
            if (!post.status.isSuccess()) {
                return Result.failure(Exception("HTTP ${post.status.value}"))
            }
            val afterHtml = post.bodyAsText()
            val after = BasementSync.checkBasement(afterHtml, preferences)
            if (after == before) {
                Result.failure(Exception("Failed to pass basement test."))
            } else {
                Result.success(after)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
