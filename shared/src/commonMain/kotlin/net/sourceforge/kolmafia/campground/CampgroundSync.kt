package net.sourceforge.kolmafia.campground

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor

/**
 * Desktop CampgroundRequest.parseResponse / parseCampground hub (Phases 2331–2360).
 */
object CampgroundSync {
    private val DNA_PATTERN = Regex(
        """sample of <b>(.*?)</b> DNA""",
        RegexOption.IGNORE_CASE,
    )
    private val FURNISHING_PATTERN = Regex(
        """<b>(?:an? )?(.*?)</b>""",
        RegexOption.IGNORE_CASE,
    )
    private val COLD_MEDICINE = Regex(
        """Your next dosage of Cold Medicine will be available in ([\d,]+) turns?.*?(\d)/5""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val FREE_REST_GIF = Regex("""/rest[\da-z]+tp?_free\.gif""", RegexOption.IGNORE_CASE)
    private val HOUSING = Regex("""/rest([\da-z])(tp)?(_free)?\.gif""", RegexOption.IGNORE_CASE)

    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        character: KoLCharacter? = null,
        inventory: InventoryManager? = null,
    ) {
        if (!url.contains("campground.php", ignoreCase = true)) return
        parseCampground(url, html, preferences, character)
        val action = url.substringAfter("action=", "").substringBefore('&').lowercase()
        if (action == "pizza" || action == "makepizza") {
            return
        }
        when {
            action.contains("rest") -> parseRest(html, preferences, character, inventory)
            action.contains("inspectdwelling") -> parseFurnishings(html, preferences)
            action.contains("workshed") || action.contains("inspectkitchen") -> {
                CampgroundItemSync.syncFromHtml(html, preferences)
                parseDnaAndCmc(html, preferences)
            }
            action.contains("dnapotion") -> {
                preferences?.setInt(
                    "_dnaPotionsMade",
                    (preferences.getInt("_dnaPotionsMade", 0) + 1),
                )
            }
            action.contains("dnainject") -> {
                preferences?.setBoolean("_dnaHybrid", true)
                preferences?.setString("_dnaSyringe", "")
            }
            action.contains("spinningwheel") -> preferences?.setBoolean("_spinningWheel", true)
            action.contains("monolith") -> preferences?.setBoolean("_blackMonolithUsed", true)
            action.contains("fuelconvertor") || action.contains("drive") -> {
                CampgroundItemSync.syncAsdonFuelFromHtml(html, preferences)
            }
            action.contains("garden") || action.contains("rgarden") -> {
                character?.let { GardenSync.apply(it, html, preferences) }
                GardenCropSync.syncFromHtml(html, preferences)
                preferences?.setBoolean("_gardenHarvested", true)
            }
            action.contains("terminal") -> {
                preferences?.setBoolean(CampgroundItemSync.CAMPGROUND_HAS_SOURCE_TERMINAL_PREF, true)
            }
        }
        parseDnaAndCmc(html, preferences)
        if (!action.contains("rest")) {
            ResultProcessor.processResults(false, html, inventory, character, preferences)
        }
    }

    fun parseCampground(
        url: String,
        html: String,
        preferences: Preferences?,
        character: KoLCharacter?,
    ) {
        if (url.contains("ajax=1", ignoreCase = true)) return
        DwellingSync.applyFromHtml(html, preferences)
        CampgroundItemSync.apply(preferences, html, url, character)
        character?.let { GardenSync.apply(it, html, preferences) }
        if (html.contains("action=bookshelf", ignoreCase = true) ||
            html.contains("bookshelf.gif", ignoreCase = true)
        ) {
            character?.setCampground(hasBookshelf = true)
        }
        // Free-rest availability from housing gif
        if (FREE_REST_GIF.containsMatchIn(html) || HOUSING.find(html)?.groupValues?.getOrNull(3)?.isNotEmpty() == true) {
            preferences?.setBoolean("_freeRestsAvailable", true)
        }
        parseDnaAndCmc(html, preferences)
    }

    fun parseRest(
        html: String,
        preferences: Preferences?,
        character: KoLCharacter? = null,
        inventory: InventoryManager? = null,
    ) {
        preferences?.setInt("timesRested", preferences.getInt("timesRested", 0) + 1)
        if (html.contains("_free", ignoreCase = true) ||
            html.contains("didn't cost", ignoreCase = true) ||
            html.contains("free rest", ignoreCase = true)
        ) {
            preferences?.setInt(
                "_freeRestsUsed",
                preferences.getInt("_freeRestsUsed", 0) + 1,
            )
        }
        ResultProcessor.processResults(false, html, inventory, character, preferences)
    }

    fun parseFurnishings(html: String, preferences: Preferences?) {
        preferences ?: return
        val names = FURNISHING_PATTERN.findAll(html)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotEmpty() && !it.equals("Furnishings", ignoreCase = true) }
            .take(12)
            .toList()
        if (names.isNotEmpty()) {
            preferences.setString("_campgroundFurnishings", names.joinToString("|"))
        }
    }

    fun parseDnaAndCmc(html: String, preferences: Preferences?) {
        preferences ?: return
        DNA_PATTERN.find(html)?.groupValues?.get(1)?.trim()?.let {
            preferences.setString("dnaSyringe", it)
            preferences.setString("_dnaSyringe", it)
        }
        COLD_MEDICINE.find(html)?.let { m ->
            val turns = m.groupValues[1].replace(",", "").toIntOrNull() ?: return@let
            val consults = m.groupValues[2].toIntOrNull() ?: return@let
            preferences.setInt("_nextColdMedicineConsult", turns)
            preferences.setInt("_coldMedicineConsults", consults)
        }
        if (html.contains("Looks like the doctors are out for the day.", ignoreCase = true)) {
            preferences.setInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, 10815)
        }
    }

    /** Desktop CampgroundRequest.getAdventuresUsed for action=rest. */
    fun getAdventuresUsed(url: String, freeRestsRemaining: Int): Int {
        if (!url.contains("campground.php", ignoreCase = true)) return 0
        if (!url.contains("action=rest", ignoreCase = true)) return 0
        return if (freeRestsRemaining > 0) 0 else 1
    }

    fun freeRestsRemaining(preferences: Preferences?): Int {
        val used = preferences?.getInt("_freeRestsUsed", 0) ?: 0
        val available = if (preferences?.getBoolean("_freeRestsAvailable", false) == true) 1 else 0
        // Desktop tracks freeRestsRemaining more deeply; approximate with unused free rests
        val totalFree = preferences?.getInt("freeRestsAvailable", available) ?: available
        return (totalFree - used).coerceAtLeast(0)
    }
}
