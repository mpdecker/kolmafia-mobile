package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences

class RequestLoggerTest {

    private lateinit var prefs: Preferences
    private lateinit var logger: SessionLogger

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
        logger = SessionLogger(prefs, GameEventBus())
        RequestLogger.currentRound = { 0 }
        ChoiceCombatAshState.reset()
    }

    @Test
    fun skipsApiAndCharpane() {
        assertFalse(RequestLogger.registerRequest("api.php?what=status", logger, prefs))
        assertFalse(RequestLogger.registerRequest("charpane.php", logger, prefs))
        assertTrue(logger.recentLines().isEmpty())
    }

    @Test
    fun shopVisitAndBuy() {
        assertTrue(RequestLogger.registerRequest("shop.php?whichshop=generalstore", logger, prefs))
        assertTrue(logger.recentLines().any { it.contains("Visiting generalstore") })

        assertTrue(
            RequestLogger.registerRequest(
                "shop.php?whichshop=fdkol&action=buyitem&quantity=2&whichrow=1",
                logger,
                prefs,
            ),
        )
        assertTrue(logger.recentLines().any { it.startsWith("buy 2 from fdkol") })
    }

    @Test
    fun placeVisit() {
        assertTrue(
            RequestLogger.registerRequest(
                "place.php?whichplace=airport_hot&action=airport4_questhub",
                logger,
                prefs,
            ),
        )
        assertTrue(logger.recentLines().any { it.contains("WLF Bunker") })
    }

    @Test
    fun choiceGenericAndIotm() {
        assertTrue(RequestLogger.registerRequest("choice.php?whichchoice=123&option=2", logger, prefs))
        assertTrue(logger.recentLines().any { it == "choice 123/2" })

        assertTrue(
            RequestLogger.registerRequest(
                "choice.php?whichchoice=1399&option=1&which=Strength",
                logger,
                prefs,
            ),
        )
        assertTrue(logger.recentLines().any { it.contains("Deck of Every Card") })
    }

    @Test
    fun useEatDrinkEquipSkill() {
        assertTrue(RequestLogger.registerRequest("inv_eat.php?whichitem=1&quantity=1", logger, prefs))
        assertTrue(logger.recentLines().any { it.startsWith("eat ") })

        assertTrue(RequestLogger.registerRequest("inv_booze.php?whichitem=1", logger, prefs))
        assertTrue(logger.recentLines().any { it.startsWith("drink ") })

        assertTrue(
            RequestLogger.registerRequest(
                "inv_equip.php?which=2&action=equip&whichitem=1",
                logger,
                prefs,
            ),
        )
        assertTrue(logger.recentLines().any { it.startsWith("equip ") })

        assertTrue(
            RequestLogger.registerRequest(
                "runskillz.php?whichskill=3004&quantity=1",
                logger,
                prefs,
            ),
        )
        assertTrue(logger.recentLines().any { it.startsWith("cast ") })
    }

    @Test
    fun campgroundAndClosetStorage() {
        assertTrue(RequestLogger.registerRequest("campground.php?action=rest", logger, prefs))
        assertTrue(logger.recentLines().any { it == "rest" })

        assertTrue(
            RequestLogger.registerRequest(
                "inventory.php?action=closetpush&whichitem=1",
                logger,
                prefs,
            ),
        )
        assertTrue(logger.recentLines().any { it.contains("closet") })

        assertTrue(RequestLogger.registerRequest("storage.php?action=take", logger, prefs))
        assertTrue(logger.recentLines().any { it.contains("storage") || it.contains("Hagnk") })
    }

    @Test
    fun adventureSnarfblat() {
        prefs.setString(Preferences.LAST_LOCATION, "The Haunted Pantry")
        assertTrue(
            RequestLogger.registerRequest("adventure.php?snarfblat=15", logger, prefs),
        )
        assertTrue(logger.recentLines().any { it.contains("Haunted Pantry") })
    }

    @Test
    fun midFightIgnoresNonFight() {
        RequestLogger.currentRound = { 3 }
        assertFalse(RequestLogger.registerRequest("shop.php?whichshop=x", logger, prefs))
        assertTrue(logger.recentLines().isEmpty())
    }

    @Test
    fun lastUrlTracked() {
        RequestLogger.registerRequest("guild.php?action=chal", logger, prefs)
        assertEquals("guild.php?action=chal", RequestLogger.lastURLString)
    }

    @Test
    fun zapAndUneffect() {
        assertTrue(RequestLogger.registerRequest("wand.php?whichitem=42", logger, prefs))
        assertTrue(logger.recentLines().any { it.startsWith("zap ") })
        assertTrue(
            RequestLogger.registerRequest("uneffect.php?whicheffect=12", logger, prefs),
        )
        assertTrue(logger.recentLines().any { it.startsWith("uneffect ") })
    }

    @Test
    fun coinmasterAndIotmDepth() {
        assertTrue(RequestLogger.registerRequest("shop.php?whichshop=hermit", logger, prefs))
        assertTrue(logger.recentLines().any { it.contains("Hermit") })

        assertTrue(
            RequestLogger.registerRequest(
                "choice.php?whichchoice=720&option=1&plant=5",
                logger,
                prefs,
            ),
        )
        assertTrue(logger.recentLines().any { it.contains("florist", ignoreCase = true) || it.contains("Planting") })

        assertTrue(
            RequestLogger.registerRequest(
                "choice.php?whichchoice=1420&option=1&pocket=42",
                logger,
                prefs,
            ),
        )
        assertTrue(logger.recentLines().any { it.contains("pocket 42") })

        assertTrue(
            RequestLogger.registerRequest("inventory.php?action=pocket", logger, prefs),
        )
        assertTrue(logger.recentLines().any { it.contains("Cargo Cultist Shorts") })
    }

    @Test
    fun createAndMallLongTail() {
        assertTrue(
            RequestLogger.registerRequest(
                "craft.php?mode=cook&target=1&qty=2",
                logger,
                prefs,
            ),
        )
        assertTrue(logger.recentLines().any { it.startsWith("create ") })

        assertTrue(RequestLogger.registerRequest("mallstore.php?action=buy", logger, prefs))
        assertTrue(logger.recentLines().any { it.contains("mall") })

        assertTrue(RequestLogger.registerRequest("familiar.php?action=newfam&newfam=1", logger, prefs))
        assertTrue(logger.recentLines().any { it.startsWith("familiar ") })
    }
}
