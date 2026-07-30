package net.sourceforge.kolmafia.data

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class DefaultsDatabaseTest {

    @BeforeTest
    fun setUp() {
        DefaultsDatabase.resetForTest()
    }

    @AfterTest
    fun tearDown() {
        DefaultsDatabase.resetForTest()
    }

    @Test
    fun load_readsBundledDefaultsFile() = runBlocking {
        DefaultsDatabase.load()
        assertTrue(DefaultsDatabase.isLoaded)
        assertTrue(DefaultsDatabase.loadedEntryCount > 3000)
    }

    @Test
    fun load_spotChecksKnownDefaults() = runBlocking {
        DefaultsDatabase.load()
        assertEquals("savecontinue", DefaultsDatabase.getString("oceanAction"))
        assertEquals("manual", DefaultsDatabase.getString("oceanDestination"))
        assertTrue(DefaultsDatabase.getBoolean("abortOnChoiceWhenNotInChoice"))
    }

    @Test
    fun parseForTest_skipsVersionCommentsAndParsesAttributes() {
        val fixture = """
            2
            #
            # Attributes
            global	abortOnChoiceWhenNotInChoice	true
            user	oasisAvailable	false	roa
            user	noodleSummons	0	ld
            user	oldPref	value	deprecated: use newPref instead

        """.trimIndent()

        val snapshot = DefaultsDatabase.parseForTest(fixture)
        assertEquals(4, snapshot.entries.size)

        val abort = snapshot.entries["abortOnChoiceWhenNotInChoice"]
        assertNotNull(abort)
        assertEquals(DefaultsDatabase.Scope.GLOBAL, abort.scope)
        assertEquals("true", abort.value)

        val oasis = snapshot.entries["oasisAvailable"]
        assertNotNull(oasis)
        assertTrue("roa" in oasis.attributes)
        assertTrue(snapshot.resetOnAscension.contains("oasisAvailable"))

        val noodle = snapshot.entries["noodleSummons"]
        assertNotNull(noodle)
        assertTrue(snapshot.legacyDailies.contains("noodleSummons"))

        val oldPref = snapshot.entries["oldPref"]
        assertNotNull(oldPref)
        assertEquals("use newPref instead", oldPref.deprecationNotice)
    }

    @Test
    fun parseForTest_userOverridesGlobalOnNameCollision() {
        val fixture = """
            global	sharedPref	globalValue
            user	sharedPref	userValue

        """.trimIndent()

        val snapshot = DefaultsDatabase.parseForTest(fixture)
        assertEquals("userValue", snapshot.entries["sharedPref"]?.value)
    }

    @Test
    fun getInt_parsesNumericDefaults() {
        DefaultsDatabase.injectForTest(
            DefaultsDatabase.parseForTest(
                """
                user	coinMasterIndex	1
                user	lastGlobalCounterDay	-1
                user	notAnInt	abc
                """.trimIndent(),
            ),
        )
        assertEquals(1, DefaultsDatabase.getInt("coinMasterIndex"))
        assertEquals(-1, DefaultsDatabase.getInt("lastGlobalCounterDay"))
        assertEquals(0, DefaultsDatabase.getInt("notAnInt"))
        assertEquals(0, DefaultsDatabase.getInt("missing"))
    }

    @Test
    fun preferences_usesBundledDefaultWhenKeyUnset() = runBlocking {
        DefaultsDatabase.load()
        val prefs = Preferences(MapSettings())

        assertEquals("savecontinue", prefs.getString("oceanAction"))
        assertEquals("manual", prefs.getString("oceanDestination"))
        assertTrue(prefs.getBoolean("abortOnChoiceWhenNotInChoice"))
    }

    @Test
    fun preferences_explicitDefaultOverridesBundledDefault() = runBlocking {
        DefaultsDatabase.load()
        val prefs = Preferences(MapSettings())

        assertEquals("custom", prefs.getString("oceanAction", "custom"))
        assertFalse(prefs.getBoolean("abortOnChoiceWhenNotInChoice", false))
        assertEquals(99, prefs.getInt("coinMasterIndex", 99))
    }

    @Test
    fun preferences_storedValueOverridesBundledDefault() = runBlocking {
        DefaultsDatabase.load()
        val prefs = Preferences(MapSettings())
        prefs.setString("oceanAction", "stop")

        assertEquals("stop", prefs.getString("oceanAction"))
    }
}
