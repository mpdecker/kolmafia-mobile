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

    @Test
    fun seedMissingDefaults_writesAllMissingKeysFromFixture() {
        DefaultsDatabase.injectForTest(
            DefaultsDatabase.parseForTest(
                """
                global	boolPref	true
                user	intPref	42
                user	strPref	savecontinue
                """.trimIndent(),
            ),
        )
        val prefs = Preferences(MapSettings())

        assertEquals(3, DefaultsDatabase.seedMissingDefaults(prefs))
        assertTrue(prefs.hasKey("boolPref"))
        assertTrue(prefs.hasKey("intPref"))
        assertTrue(prefs.hasKey("strPref"))
        assertTrue(prefs.getBoolean("boolPref"))
        assertEquals(42, prefs.getInt("intPref"))
        assertEquals("savecontinue", prefs.getString("strPref"))
    }

    @Test
    fun seedMissingDefaults_isIdempotent() {
        DefaultsDatabase.injectForTest(
            DefaultsDatabase.parseForTest(
                """
                user	a	1
                user	b	two
                """.trimIndent(),
            ),
        )
        val prefs = Preferences(MapSettings())

        assertEquals(2, DefaultsDatabase.seedMissingDefaults(prefs))
        assertEquals(0, DefaultsDatabase.seedMissingDefaults(prefs))
    }

    @Test
    fun seedMissingDefaults_preservesExplicitValues() {
        DefaultsDatabase.injectForTest(
            DefaultsDatabase.parseForTest(
                """
                user	oceanAction	savecontinue
                user	otherPref	false
                """.trimIndent(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setString("oceanAction", "stop")

        assertEquals(1, DefaultsDatabase.seedMissingDefaults(prefs))
        assertEquals("stop", prefs.getString("oceanAction"))
        assertFalse(prefs.getBoolean("otherPref"))
    }

    @Test
    fun seedMissingDefaults_typeHeuristic() {
        DefaultsDatabase.injectForTest(
            DefaultsDatabase.parseForTest(
                """
                user	boolTrue	true
                user	boolFalse	FALSE
                user	intZero	0
                user	emptyStr	
                """.trimIndent(),
            ),
        )
        val prefs = Preferences(MapSettings())
        DefaultsDatabase.seedMissingDefaults(prefs)

        assertTrue(prefs.getBoolean("boolTrue"))
        assertFalse(prefs.getBoolean("boolFalse"))
        assertEquals(0, prefs.getInt("intZero"))
        assertEquals("", prefs.getString("emptyStr"))
    }

    @Test
    fun seedMissingDefaults_afterLoad_persistsOceanAction() = runBlocking {
        DefaultsDatabase.load()
        val prefs = Preferences(MapSettings())

        assertFalse(prefs.hasKey("oceanAction"))
        assertTrue(DefaultsDatabase.seedMissingDefaults(prefs) > 0)
        assertTrue(prefs.hasKey("oceanAction"))
        assertEquals("savecontinue", prefs.getString("oceanAction"))
    }

    @Test
    fun resetToDefault_restoresTypedValues() {
        DefaultsDatabase.injectForTest(
            DefaultsDatabase.parseForTest(
                """
                user	oasisAvailable	false	roa
                user	coinMasterIndex	1
                """.trimIndent(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("oasisAvailable", true)
        prefs.setInt("coinMasterIndex", 99)

        assertTrue(DefaultsDatabase.resetToDefault(prefs, "oasisAvailable"))
        assertTrue(DefaultsDatabase.resetToDefault(prefs, "coinMasterIndex"))
        assertFalse(DefaultsDatabase.resetToDefault(prefs, "missing"))

        assertFalse(prefs.getBoolean("oasisAvailable"))
        assertEquals(1, prefs.getInt("coinMasterIndex"))
    }

    @Test
    fun resetOnAscensionPrefs_resetsOnlyRoa() {
        DefaultsDatabase.injectForTest(
            DefaultsDatabase.parseForTest(
                """
                user	oasisAvailable	false	roa
                user	oceanAction	savecontinue
                """.trimIndent(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("oasisAvailable", true)
        prefs.setString("oceanAction", "stop")

        assertEquals(1, DefaultsDatabase.resetOnAscensionPrefs(prefs))
        assertFalse(prefs.getBoolean("oasisAvailable"))
        assertEquals("stop", prefs.getString("oceanAction"))
    }

    @Test
    fun resetOnFightPrefs_resetsRofSet() {
        DefaultsDatabase.injectForTest(
            DefaultsDatabase.parseForTest(
                """
                user	_douseFoeSuccess	false	rof
                user	_lastCombatActions		rof
                user	oceanAction	savecontinue
                """.trimIndent(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("_douseFoeSuccess", true)
        prefs.setString("_lastCombatActions", "attack")
        prefs.setString("oceanAction", "stop")

        assertEquals(2, DefaultsDatabase.resetOnFightPrefs(prefs))
        assertFalse(prefs.getBoolean("_douseFoeSuccess"))
        assertEquals("", prefs.getString("_lastCombatActions"))
        assertEquals("stop", prefs.getString("oceanAction"))
    }

    @Test
    fun applyAscensionResetIfNeeded_firstLoginNoReset() {
        DefaultsDatabase.injectForTest(
            DefaultsDatabase.parseForTest(
                """
                user	oasisAvailable	false	roa
                """.trimIndent(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("oasisAvailable", true)

        assertFalse(DefaultsDatabase.applyAscensionResetIfNeeded(prefs, 5))
        assertTrue(prefs.getBoolean("oasisAvailable"))
        assertEquals(5, prefs.getInt(Preferences.LAST_ASCENSION_NUMBER))
    }

    @Test
    fun applyAscensionResetIfNeeded_detectsIncrease() {
        DefaultsDatabase.injectForTest(
            DefaultsDatabase.parseForTest(
                """
                user	oasisAvailable	false	roa
                """.trimIndent(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setInt(Preferences.LAST_ASCENSION_NUMBER, 5)
        prefs.setBoolean("oasisAvailable", true)

        assertTrue(DefaultsDatabase.applyAscensionResetIfNeeded(prefs, 6))
        assertFalse(prefs.getBoolean("oasisAvailable"))
        assertEquals(6, prefs.getInt(Preferences.LAST_ASCENSION_NUMBER))
    }

    @Test
    fun applyAscensionResetIfNeeded_sameAscensionNoReset() {
        DefaultsDatabase.injectForTest(
            DefaultsDatabase.parseForTest(
                """
                user	oasisAvailable	false	roa
                """.trimIndent(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setInt(Preferences.LAST_ASCENSION_NUMBER, 6)
        prefs.setBoolean("oasisAvailable", true)

        assertFalse(DefaultsDatabase.applyAscensionResetIfNeeded(prefs, 6))
        assertTrue(prefs.getBoolean("oasisAvailable"))
        assertEquals(6, prefs.getInt(Preferences.LAST_ASCENSION_NUMBER))
    }

    @Test
    fun isDaily_underscorePrefix() {
        DefaultsDatabase.injectForTest(
            DefaultsDatabase.parseForTest(
                """
                user	_shortOrderCookCharge	0
                """.trimIndent(),
            ),
        )
        assertTrue(DefaultsDatabase.isDaily("_foo"))
        assertFalse(DefaultsDatabase.isDaily("_shortOrderCookCharge"))
        assertFalse(DefaultsDatabase.isDaily("oceanAction"))
    }

    @Test
    fun isDaily_legacyDailies() {
        DefaultsDatabase.injectForTest(
            DefaultsDatabase.parseForTest(
                """
                user	noodleSummons	0	ld
                """.trimIndent(),
            ),
        )
        assertTrue(DefaultsDatabase.isDaily("noodleSummons"))
    }

    @Test
    fun resetDailies_resetsKnownDefaults() {
        DefaultsDatabase.injectForTest(
            DefaultsDatabase.parseForTest(
                """
                user	_dailyPref	false
                """.trimIndent(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("_dailyPref", true)

        assertEquals(1, DefaultsDatabase.resetDailies(prefs))
        assertFalse(prefs.getBoolean("_dailyPref"))
    }

    @Test
    fun resetDailies_deletesUnknownUnderscorePref() {
        DefaultsDatabase.injectForTest(
            DefaultsDatabase.parseForTest(
                """
                user	oceanAction	savecontinue
                """.trimIndent(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setString("_orphan", "gone")

        assertEquals(1, DefaultsDatabase.resetDailies(prefs))
        assertFalse(prefs.hasKey("_orphan"))
    }

    @Test
    fun resetDailies_skipsNonDaily() {
        DefaultsDatabase.injectForTest(
            DefaultsDatabase.parseForTest(
                """
                user	oceanAction	savecontinue
                """.trimIndent(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setString("oceanAction", "stop")

        assertEquals(0, DefaultsDatabase.resetDailies(prefs))
        assertEquals("stop", prefs.getString("oceanAction"))
    }

    @Test
    fun resetPerRolloverPrefs_resetsHardcodedSet() {
        DefaultsDatabase.injectForTest(
            DefaultsDatabase.parseForTest(
                """
                user	ascensionsToday	0
                user	potatoAlarmClockUsed	false
                """.trimIndent(),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setInt("ascensionsToday", 3)
        prefs.setBoolean("potatoAlarmClockUsed", true)

        assertEquals(2, DefaultsDatabase.resetPerRolloverPrefs(prefs))
        assertEquals(0, prefs.getInt("ascensionsToday"))
        assertFalse(prefs.getBoolean("potatoAlarmClockUsed"))
    }
}
