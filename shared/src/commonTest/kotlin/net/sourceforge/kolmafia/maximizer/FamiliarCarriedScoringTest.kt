package net.sourceforge.kolmafia.maximizer

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FamiliarCarriedScoringTest {

    private val db = object : GameDatabase() {
        override fun item(name: String) = when (name) {
            "volley-hat" -> ItemData(
                1, name, "", "", ItemPrimaryUse.HAT, emptySet(), emptySet(), 0, null,
            )
            "stat-hat" -> ItemData(
                2, name, "", "", ItemPrimaryUse.HAT, emptySet(), emptySet(), 0, null,
            )
            else -> null
        }

        override fun itemModifier(name: String) = when (name) {
            "volley-hat" -> net.sourceforge.kolmafia.data.ModifierEntry(
                "Item", name, """Mysticality: +99, Familiar Effect: "1xVolley, cap 12"""",
            )
            "stat-hat" -> net.sourceforge.kolmafia.data.ModifierEntry(
                "Item", name, "Mysticality: +99",
            )
            else -> null
        }
    }

    @Test
    fun translateFamiliarEffect_mapsVolleyAndCap() {
        val translated = FamiliarCarriedScoring.translateFamiliarEffect("1xVolley, cap 12")
        assertTrue(translated.contains("Volleyball: 1"))
        assertTrue(translated.contains("Familiar Weight Cap: 12"))
    }

    @Test
    fun score_hatrackUsesFamiliarEffectNotItemStats() = runBlocking {
        ModifierDatabase.load()
        val experience = FamiliarCarriedScoring.score(
            FamiliarCarryRules.HATRACK_RACE,
            "volley-hat",
            DoubleModifier.EXPERIENCE,
            db,
            familiarWeight = 10,
        )
        val mysticality = FamiliarCarriedScoring.score(
            FamiliarCarryRules.HATRACK_RACE,
            "volley-hat",
            DoubleModifier.MYS,
            db,
            familiarWeight = 10,
        )
        assertTrue(experience > 0.0)
        assertEquals(0.0, mysticality)
    }

    @Test
    fun score_hatrackWithoutFamiliarEffectScoresZeroForMys() {
        val score = FamiliarCarriedScoring.score(
            FamiliarCarryRules.HATRACK_RACE,
            "stat-hat",
            DoubleModifier.MYS,
            db,
        )
        assertEquals(0.0, score)
    }
}
