package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UseItemGiftPackageSyncTest {

    private val fromHtml =
        """<p>From: <b><a class=nounder href="showplayer.php?who=42">Alice</a></b>"""

    private fun logger(): SessionLogger = SessionLogger(Preferences(MapSettings()), GameEventBus())

    @Test
    fun giftPackage_logsOpeningFromSender() {
        val logger = logger()
        UseItemGiftPackageSync.parse(
            responseText = fromHtml,
            itemId = 1167,
            sessionLogger = logger,
            itemName = { "plain brown wrapper" },
            isGiftPackage = { true },
        )
        assertTrue(logger.recentLines().any { it == "Opening plain brown wrapper from Alice" })
    }

    @Test
    fun nonPackage_doesNotLog() {
        val logger = logger()
        UseItemGiftPackageSync.parse(
            responseText = fromHtml,
            itemId = 1,
            sessionLogger = logger,
            isGiftPackage = { false },
        )
        assertFalse(logger.recentLines().any { it.startsWith("Opening ") })
    }

    @Test
    fun packageWithoutSender_doesNotLog() {
        val logger = logger()
        UseItemGiftPackageSync.parse(
            responseText = "You open the package.",
            itemId = 1167,
            sessionLogger = logger,
            isGiftPackage = { true },
        )
        assertFalse(logger.recentLines().any { it.startsWith("Opening ") })
    }

    @Test
    fun isGiftPackage_readsPackageSecondaryUse() {
        ItemDatabase.resetForTest()
        try {
            ItemDatabase.registerForTest(
                ItemData(
                    id = 1167,
                    name = "plain brown wrapper",
                    descId = "546961999",
                    image = "plainbrown.gif",
                    primaryUse = ItemPrimaryUse.USABLE,
                    secondaryUses = setOf("package"),
                    access = setOf('g'),
                    autosellPrice = 0,
                    plural = null,
                ),
            )
            assertTrue(ItemDatabase.isGiftPackage(1167))
            assertFalse(ItemDatabase.isGiftPackage(1))
            val logger = logger()
            UseItemGiftPackageSync.parse(fromHtml, 1167, logger)
            assertTrue(logger.recentLines().any { it == "Opening plain brown wrapper from Alice" })
        } finally {
            ItemDatabase.resetForTest()
        }
    }
}
