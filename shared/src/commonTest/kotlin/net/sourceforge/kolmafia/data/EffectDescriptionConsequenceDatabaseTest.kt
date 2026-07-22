package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.EffectQuality

class EffectDatabaseTest {

    @AfterTest
    fun tearDown() {
        EffectDatabase.resetForTest()
    }

    @Test
    fun getByDescId_returnsRegisteredEffect() {
        EffectDatabase.registerForTest(
            EffectData(
                id = 1,
                name = "Antihangover",
                image = "martini.gif",
                descId = "eadafbbe6d035eb5a36408dfbb1c85cc",
                quality = EffectQuality.NEUTRAL,
                attributes = emptySet(),
            ),
        )
        assertEquals(
            "Antihangover",
            EffectDatabase.getByDescId("eadafbbe6d035eb5a36408dfbb1c85cc")?.name,
        )
        assertNull(EffectDatabase.getByDescId("missing"))
    }

    @Test
    fun load_indexesByDescId() = runTest {
        EffectDatabase.load()
        assertEquals(
            "Grafted",
            EffectDatabase.getByDescId("003e2e2d7de4a3fb2982c7615b1cbcdc")?.name,
        )
    }
}

class EffectDescriptionConsequenceDatabaseTest {

    @AfterTest
    fun tearDown() {
        EffectDatabase.resetForTest()
        EffectDescriptionConsequenceDatabase.resetForTest()
    }

    @Test
    fun parseForTest_loadsExpressionAndModsRules() {
        EffectDatabase.registerForTest(
            EffectData(
                id = 615,
                name = "Antihangover",
                image = "martini.gif",
                descId = "antihangover-desc",
                quality = EffectQuality.NEUTRAL,
                attributes = emptySet(),
            ),
        )
        EffectDatabase.registerForTest(
            EffectData(
                id = 2720,
                name = "Buzzed on Distillate",
                image = "chinsweat.gif",
                descId = "distillate-desc",
                quality = EffectQuality.NEUTRAL,
                attributes = emptySet(),
            ),
        )
        val parsed = EffectDescriptionConsequenceDatabase.parseForTest(
            """
            DESC_EFFECT	Antihangover	Moxie \\+(\d+)	_antihangoverBonus=$1
            DESC_EFFECT	Buzzed on Distillate		currentDistillateMods=mods
            """.trimIndent(),
        )
        assertEquals(1, parsed["antihangover-desc"]?.size)
        assertEquals(1, parsed["distillate-desc"]?.size)
    }

    @Test
    fun parseForTest_skipsUnknownEffect() {
        val parsed = EffectDescriptionConsequenceDatabase.parseForTest(
            """
            DESC_EFFECT	Unknown Effect	foo=bar	foo=bar
            """.trimIndent(),
        )
        assertEquals(0, parsed.size)
    }

    @Test
    fun parseForTest_indexesDualRulesUnderSameDescId() {
        EffectDatabase.registerForTest(
            EffectData(
                id = 2822,
                name = "Citizen of a Zone",
                image = "flag1.gif",
                descId = "citizen-desc",
                quality = EffectQuality.NEUTRAL,
                attributes = emptySet(),
            ),
        )
        val parsed = EffectDescriptionConsequenceDatabase.parseForTest(
            """
            DESC_EFFECT	Citizen of a Zone	Citizen of ([^<]*)<	_citizenZone=$1
            DESC_EFFECT	Citizen of a Zone		_citizenZoneMods=mods
            """.trimIndent(),
        )
        assertEquals(2, parsed["citizen-desc"]?.size)
    }
}
