package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.adventure.ShadowRift
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdventureDatabaseUrlMapTest {

    @AfterTest
    fun tearDown() {
        AdventureDatabase.resetForTest()
    }

    @Test
    fun zone_parsesFormSourceFromUrlParams() {
        val snarf = AdventureZone(
            zoneName = "Woods",
            urlParams = "adventure=15",
            locationName = "The Spooky Forest",
            environment = "outdoor",
            diffLevel = "low",
            statRequirement = 0,
            goals = emptyList(),
            isOverdrunk = false,
            noWander = false,
        )
        assertEquals("adventure.php", snarf.formSource)
        assertEquals("15", snarf.adventureId)
        assertEquals("15", snarf.snarfblat)

        val place = AdventureZone(
            zoneName = "Shadow Rift",
            urlParams = "place=shadow_rift",
            locationName = ShadowRift.WOODS.adventureName,
            environment = "outdoor",
            diffLevel = "mid",
            statRequirement = 0,
            goals = emptyList(),
            isOverdrunk = false,
            noWander = true,
        )
        assertEquals("place.php", place.formSource)
        assertEquals("shadow_rift", place.adventureId)
        assertNull(place.snarfblat)
        assertTrue(place.isShadowRift)

        val loc = place.toLocation()
        assertEquals("place.php", loc.formSource)
        assertEquals("shadow_rift", loc.adventureId)
        assertEquals(ShadowRift.WOODS.adventureName, loc.name)
    }

    @Test
    fun getAdventureByURL_snarfblatAndShadowRift() {
        AdventureDatabase.injectForTest(
            AdventureZone(
                zoneName = "Woods",
                urlParams = "adventure=15",
                locationName = "The Spooky Forest",
                environment = "outdoor",
                diffLevel = "low",
                statRequirement = 0,
                goals = emptyList(),
                isOverdrunk = false,
                noWander = false,
            ),
        )
        AdventureDatabase.injectForTest(
            AdventureZone(
                zoneName = "Shadow Rift",
                urlParams = "place=shadow_rift",
                locationName = ShadowRift.BEACH.adventureName,
                environment = "outdoor",
                diffLevel = "mid",
                statRequirement = 0,
                goals = emptyList(),
                isOverdrunk = false,
                noWander = true,
            ),
        )

        assertEquals("The Spooky Forest", AdventureDatabase.getAdventureByURL("adventure.php?snarfblat=15")?.locationName)
        assertEquals(
            ShadowRift.BEACH.adventureName,
            AdventureDatabase.getAdventureByURL(
                "place.php?whichplace=desertbeach&action=db_shadowrift",
            )?.locationName,
        )
        assertNotNull(AdventureDatabase.getByName(ShadowRift.BEACH.adventureName))
    }

    @Test
    fun loadFromText_registersPlaceAndCasino() {
        AdventureDatabase.loadFromText(
            """
            Casino	casino=1	DiffLevel: none Env: none Stat: 0	Goat Party
            Manor0	place=manor4_chamberboss	DiffLevel: none Env: none Stat: 0	Summoning Chamber
            """.trimIndent(),
        )
        val goat = AdventureDatabase.getByName("Goat Party")
        assertEquals("casino.php", goat?.formSource)
        assertEquals("1", goat?.adventureId)
        val chamber = AdventureDatabase.getByName("Summoning Chamber")
        assertEquals("place.php", chamber?.formSource)
        assertEquals("manor4_chamberboss", chamber?.adventureId)
    }
}
