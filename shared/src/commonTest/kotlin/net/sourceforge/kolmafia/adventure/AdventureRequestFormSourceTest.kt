package net.sourceforge.kolmafia.adventure

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdventureRequestFormSourceTest {

    @Test
    fun adventure_postsPlacePhpFields() = runTest {
        var postedPath = ""
        var postedBody = ""
        val client = HttpClient(MockEngine { request ->
            postedPath = request.url.encodedPath
            postedBody = request.body.toString()
            respond("<!--adventure-->You acquire nothing.", HttpStatusCode.OK)
        })
        val request = AdventureRequest(client)
        val location = AdventureLocation(
            id = "shadow_rift",
            name = ShadowRift.PLAINS.adventureName,
            zone = "Shadow Rift",
            formSource = "place.php",
            adventureId = "shadow_rift",
        )
        // Without prefs, form builder still resolves place via adventure name... 
        // Actually shadowRift needs adventureName match; prefs null means ingress empty → place.php
        val form = request.resolveForm(location)
        // Without prefs and matching name, ShadowRift.findAdventureName works
        assertEquals("place.php", form.formSource)
        assertEquals("plains", form.fields["whichplace"])

        val result = request.adventure(location)
        assertTrue(result.isSuccess)
        assertTrue(postedPath.contains("place.php") || postedPath.endsWith("place.php"))
    }

    @Test
    fun processResults_nstowerWithoutFightFails() {
        val request = AdventureRequest(HttpClient(MockEngine { respond("") }))
        val form = AdventureFormBuilder.build("place.php", "ns_05_monster1")
        val err = request.processResults(form, "You stand around.", "place.php?whichplace=nstower")
        assertEquals("You can't adventure there.", err)
    }

    @Test
    fun recommendCellarSquare_findsFirstDark() {
        val request = AdventureRequest(HttpClient(MockEngine { respond("") }))
        assertEquals(1, request.recommendCellarSquare("0000000000000000000000000"))
        assertEquals(3, request.recommendCellarSquare("1100000000000000000000000"))
        assertEquals(0, request.recommendCellarSquare("1".repeat(25)))
    }
}
