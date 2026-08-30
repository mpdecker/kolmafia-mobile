package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.data.BountyData
import net.sourceforge.kolmafia.data.BountyDatabase
import net.sourceforge.kolmafia.data.BountyType
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BountyHunterHunterRequestTest {

    @BeforeTest
    fun seed() {
        BountyDatabase.resetForTest()
        BountyDatabase.registerForTest(
            BountyData(
                name = "blob of white goo",
                plural = "blobs of white goo",
                type = BountyType.EASY,
                image = "goo.gif",
                count = 10,
                monster = "goo blob",
                bestLocation = "The Haunted Bathroom",
            ),
        )
    }

    @Test
    fun parseVisit_untakenEasyBounty() {
        val prefs = Preferences(MapSettings())
        // Single-line desktop-shaped snippet (itemimages … width … N plural from … takelow)
        val html =
            """Easy Bounty: <img src=/images/itemimages/goo.gif width=30 height=30>10 blobs of white goo from The Haunted Bathroom <a href=bounty.php?action=takelow>takelow</a> You have <b>3</b> filthy lucre"""
        BountyHunterHunterRequest.parseResponse("bounty.php", html, prefs)
        assertTrue(prefs.getBoolean("bountyHunterVisited", false))
        assertEquals("blob of white goo", prefs.getString("_untakenEasyBountyItem"))
        assertEquals(3, prefs.getInt("availableFilthyLucre", 0))
    }

    @Test
    fun takeLow_setsCurrentAndLocation() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_untakenEasyBountyItem", "blob of white goo")
        BountyHunterHunterRequest.parseResponse("bounty.php?action=takelow", "", prefs)
        assertEquals("blob of white goo:0", prefs.getString("currentEasyBountyItem"))
        assertEquals("", prefs.getString("_untakenEasyBountyItem"))
        assertEquals("The Haunted Bathroom", prefs.getString("_nextBountyLocation"))
        assertEquals("The Haunted Bathroom", prefs.getString("nextAdventure"))
    }

    @Test
    fun getNameFromPlural() {
        assertEquals("blob of white goo", BountyDatabase.getName("blobs of white goo"))
        assertEquals(10, BountyDatabase.getNumber("blob of white goo"))
        assertEquals("The Haunted Bathroom", BountyDatabase.getLocation("blob of white goo"))
    }

    @Test
    fun registerRequest_visit() {
        assertTrue(
            BountyHunterHunterRequest.registerRequest("bounty.php", Preferences(MapSettings()), null),
        )
    }
}
