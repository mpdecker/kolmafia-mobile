package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [UseItemRequest] request-time fields (`lastUrlString`, `lastFruit`,
 * `lastUntinker`) plus the follow-up page fetches parseConsumption posts after
 * a successful claymore, stuffing fluffer, or Asdon Martin install
 * (Phases 8051–8110).
 */
object UseItemRequestState {
    private val WHICHITEM = Regex("""whichitem=(\d+)""", RegexOption.IGNORE_CASE)
    private val WHICHFRUIT = Regex("""whichfruit=(\d+)""", RegexOption.IGNORE_CASE)
    private val DOWHICHITEM = Regex("""dowhichitem=(\d+)""", RegexOption.IGNORE_CASE)
    private val WHICHSIGN = Regex("""whichsign=(\d+)""", RegexOption.IGNORE_CASE)
    private val QUANTITY = Regex("""quantity=(\d+)""", RegexOption.IGNORE_CASE)

    var lastUrl: String = ""
        private set
    var lastFruitId: Int = 0
        private set
    var lastUntinkerId: Int = 0
        private set
    var lastUntinkerCount: Int = 0
        private set

    private var needsIslandRefresh: Boolean = false
    private var needsWorkshedRefresh: Boolean = false

    fun remember(
        url: String,
        count: Int = 1,
        preferences: Preferences? = null,
        inventory: InventoryManager? = null,
    ) {
        lastUrl = url
        val itemId = WHICHITEM.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return
        val qty = count.coerceAtLeast(QUANTITY.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1)
        when (itemId) {
            JACKING_MAP -> {
                lastFruitId = if (url.contains("action=addfruit", ignoreCase = true)) {
                    WHICHFRUIT.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                } else {
                    0
                }
            }
            SCREWDRIVER -> if (url.contains("action=screw", ignoreCase = true)) {
                val id = DOWHICHITEM.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                if (id <= 0) {
                    lastUntinkerId = 0
                    lastUntinkerCount = 0
                } else {
                    val owned = inventory?.getCount(id) ?: 1
                    lastUntinkerId = id
                    lastUntinkerCount = if (url.contains("untinkerall=on", ignoreCase = true)) {
                        owned.coerceAtLeast(1)
                    } else {
                        1
                    }
                }
            }
            EXPRESS_CARD -> preferences?.setBoolean("expressCardUsed", true)
            SPICE_MELANGE -> preferences?.setBoolean("spiceMelangeUsed", true)
            ULTRA_MEGA_SOUR_BALL -> preferences?.setBoolean("_ultraMegaSourBallUsed", true)
            MUNCHIES_PILL -> preferences?.increment("munchiesPillsUsed", qty)
            WHETSTONE -> preferences?.increment("whetstonesUsed", qty)
            DRINK_ME_POTION -> preferences?.increment("pendingMapReflections", qty)
            MR_STORE_2002_CATALOG, REPLICA_MR_STORE_2002_CATALOG ->
                preferences?.setBoolean("_2002MrStoreCreditsCollected", true)
            MINI_KIWI_AIOLI -> preferences?.increment("miniKiwiAiolisUsed", qty)
        }
    }

    fun signFromLastUrl(): Int? =
        WHICHSIGN.find(lastUrl)?.groupValues?.getOrNull(1)?.toIntOrNull()

    fun takeFruit(): Int {
        val id = lastFruitId
        lastFruitId = 0
        return id
    }

    fun takeUntinker(): Pair<Int, Int> {
        val taken = lastUntinkerId to lastUntinkerCount
        lastUntinkerId = 0
        lastUntinkerCount = 0
        return taken
    }

    fun clearFollowUps() {
        needsIslandRefresh = false
        needsWorkshedRefresh = false
    }

    fun markIslandRefresh() {
        needsIslandRefresh = true
    }

    fun markWorkshedRefresh() {
        needsWorkshedRefresh = true
    }

    suspend fun refreshFollowUps(client: HttpClient, preferences: Preferences?) {
        val prefs = preferences ?: run {
            clearFollowUps()
            return
        }
        if (needsIslandRefresh) {
            needsIslandRefresh = false
            val response = client.get("$KOL_BASE_URL/bigisland.php")
            if (response.status.isSuccess()) {
                IslandRequest.parseResponse("bigisland.php", response.bodyAsText(), prefs)
            }
        }
        if (needsWorkshedRefresh) {
            needsWorkshedRefresh = false
            val response = client.get("$KOL_BASE_URL/campground.php") {
                parameter("action", "workshed")
            }
            if (response.status.isSuccess()) {
                CampgroundItemSync.syncFromHtml(response.bodyAsText(), prefs)
            }
        }
    }

    private const val JACKING_MAP = 4560
    private const val SCREWDRIVER = 4926
    private const val EXPRESS_CARD = 1687
    private const val SPICE_MELANGE = 3433
    private const val ULTRA_MEGA_SOUR_BALL = 6852
    private const val MUNCHIES_PILL = 1619
    private const val WHETSTONE = 11107
    private const val DRINK_ME_POTION = 4508
    private const val MR_STORE_2002_CATALOG = 11257
    private const val REPLICA_MR_STORE_2002_CATALOG = 11280
    private const val MINI_KIWI_AIOLI = 11598
}
