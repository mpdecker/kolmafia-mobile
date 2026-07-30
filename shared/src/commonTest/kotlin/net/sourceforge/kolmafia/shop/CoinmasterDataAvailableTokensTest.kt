package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class CoinmasterDataAvailableTokensTest {

    @Test
    fun availableTokens_propertyBackedDimemaster() = runBlocking {
        val db = GameDatabase()
        db.load()
        val dimemaster = CoinmasterDatabase.findByNickname("dimemaster")
            ?: error("Dimemaster not loaded")
        val prefs = Preferences(MapSettings())
        prefs.setInt("availableDimes", 42)
        assertEquals(42, dimemaster.availableTokens(prefs, emptyMap()))
    }

    @Test
    fun availableTokens_tokenItemInventoryCount() = runBlocking {
        val db = GameDatabase()
        db.load()
        val shore = CoinmasterDatabase.findByNickname("shore")
            ?: error("Shore coinmaster not loaded")
        val scripId = ItemDatabase.getByName("Shore Inc. Ship Trip Scrip")?.id
            ?: error("Shore scrip item not loaded")
        assertEquals(7, shore.availableTokens(null, mapOf(scripId to 7)))
    }
}
