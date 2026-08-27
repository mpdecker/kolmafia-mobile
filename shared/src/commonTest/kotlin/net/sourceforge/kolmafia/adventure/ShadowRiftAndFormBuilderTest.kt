package net.sourceforge.kolmafia.adventure

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ShadowRiftTest {

    @Test
    fun findAdventureName_resolvesAllThirteen() {
        for (rift in ShadowRift.entries) {
            assertEquals(rift, ShadowRift.findAdventureName(rift.adventureName))
            assertEquals(rift, ShadowRift.findPlace(rift.place))
        }
    }

    @Test
    fun currentAction_usesFreeWhenAffinity() {
        val rift = ShadowRift.BEACH
        assertEquals("db_shadowrift", rift.currentAction(hasShadowAffinity = false))
        assertEquals("db_shadowrift_free", rift.currentAction(hasShadowAffinity = true))
        assertTrue(rift.currentUrl(true).contains("_free"))
    }

    @Test
    fun formBuilder_namedRift_postsPlacePhp() {
        val prefs = Preferences(MapSettings())
        val form = AdventureFormBuilder.updateFields(
            formSource = "place.php",
            adventureId = ShadowRift.ADVENTURE_ID,
            ctx = AdventureFormBuilder.Context(
                adventureName = ShadowRift.WOODS.adventureName,
                preferences = prefs,
            ),
        )
        assertEquals("place.php", form.formSource)
        assertEquals("woods", form.fields["whichplace"])
        assertEquals("woods_shadowrift", form.fields["action"])
        assertEquals("woods", prefs.getString(ShadowRift.INGRESS_PREF, ""))
    }

    @Test
    fun formBuilder_matchingIngress_shortcutsToSnarfblat() {
        val prefs = Preferences(MapSettings()).apply {
            setString(ShadowRift.INGRESS_PREF, "woods")
        }
        val form = AdventureFormBuilder.updateFields(
            formSource = "place.php",
            adventureId = ShadowRift.ADVENTURE_ID,
            ctx = AdventureFormBuilder.Context(
                adventureName = ShadowRift.WOODS.adventureName,
                preferences = prefs,
            ),
        )
        assertEquals("adventure.php", form.formSource)
        assertEquals(ShadowRift.SNARFBLAT, form.fields["snarfblat"])
    }

    @Test
    fun formBuilder_affinityUsesFreeAction() {
        val prefs = Preferences(MapSettings())
        val form = AdventureFormBuilder.updateFields(
            formSource = "place.php",
            adventureId = ShadowRift.ADVENTURE_ID,
            ctx = AdventureFormBuilder.Context(
                adventureName = ShadowRift.BEACH.adventureName,
                preferences = prefs,
                hasShadowAffinity = true,
            ),
        )
        assertEquals("db_shadowrift_free", form.fields["action"])
    }

    @Test
    fun ingressSync_writesPrefFromPlaceUrl() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            net.sourceforge.kolmafia.quest.ShadowRiftSync.applyIngressFromUrl(
                "place.php?whichplace=desertbeach&action=db_shadowrift",
                prefs,
            ),
        )
        assertEquals("desertbeach", prefs.getString(ShadowRift.INGRESS_PREF, ""))
        assertFalse(
            net.sourceforge.kolmafia.quest.ShadowRiftSync.applyIngressFromUrl(
                "place.php?whichplace=town&action=town_eincursion",
                prefs,
            ),
        )
    }
}

class AdventureFormBuilderTest {

    @Test
    fun casino_slotFields() {
        val form = AdventureFormBuilder.build("casino.php", "2")
        assertEquals("slot", form.fields["action"])
        assertEquals("2", form.fields["whichslot"])
    }

    @Test
    fun pyramid_includesPositionAndBomb() {
        val prefs = Preferences(MapSettings()).apply {
            setString("pyramidPosition", "3")
            setBoolean("pyramidBombUsed", true)
        }
        val form = AdventureFormBuilder.updateFields(
            "place.php",
            "pyramid_state",
            AdventureFormBuilder.Context(preferences = prefs),
        )
        assertEquals("pyramid_state3a", form.fields["action"])
        assertEquals("pyramid", form.fields["whichplace"])
    }

    @Test
    fun manor_chamberVsBoss() {
        val unfinished = AdventureFormBuilder.updateFields(
            "place.php",
            "manor4_chamberboss",
            AdventureFormBuilder.Context(manorQuestFinished = false),
        )
        assertEquals("manor4_chamberboss", unfinished.fields["action"])
        val finished = AdventureFormBuilder.updateFields(
            "place.php",
            "manor4_chamber",
            AdventureFormBuilder.Context(manorQuestFinished = true),
        )
        assertEquals("manor4_chamber", finished.fields["action"])
    }

    @Test
    fun nsTower_setsWhichplace() {
        val form = AdventureFormBuilder.build("place.php", "ns_03_hedgemaze")
        assertEquals("nstower", form.fields["whichplace"])
        assertEquals("ns_03_hedgemaze", form.fields["action"])
    }

    @Test
    fun cellar_exploreOrAutofaucet() {
        val explore = AdventureFormBuilder.updateFields(
            "cellar.php",
            "0",
            AdventureFormBuilder.Context(cellarSquare = 7, cellarAutoFaucet = false),
        )
        assertEquals("explore", explore.fields["action"])
        assertEquals("7", explore.fields["whichspot"])
        val auto = AdventureFormBuilder.updateFields(
            "cellar.php",
            "0",
            AdventureFormBuilder.Context(cellarAutoFaucet = true),
        )
        assertEquals("autofaucet", auto.fields["action"])
        assertFalse(auto.fields.containsKey("whichspot"))
    }

    @Test
    fun adventurePhp_default() {
        val form = AdventureFormBuilder.build("adventure.php", "15")
        assertEquals("15", form.fields["snarfblat"])
        assertNotNull(form.requestUrl)
    }
}
