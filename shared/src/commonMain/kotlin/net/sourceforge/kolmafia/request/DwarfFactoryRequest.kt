package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [DwarfFactoryRequest] — dwarven factory warehouse / dice / rune prefs
 * (Phases 2631–2660).
 */
object DwarfFactoryRequest {
    private val RUNE = Regex("""title="Dwarf (Digit|Word) Rune (.)"""")
    private val ITEMDESC = Regex("""descitem\((\d*)\)""")
    private val MEAT = Regex("""You (gain|lose) (\d*) Meat""")
    private val HOPPER = Regex("""<p>It currently contains (\d+) ([^.]*)\.</p>""")
    private val ACTION = Regex("""[?&]action=([^&]*)""")
    private val MATTOCK_PATTERN = Regex("""<p>Your mattock glows ((really )*) ?bright blue\.</p>""")
    private val HELMET = Regex("""<p>A small crystal lens flips down.*?</p>""")
    private val KILT = Regex("""<p>.*?our sporran.*?</p>""")
    private val COLOR = Regex("""(red|orange|yellow|green|blue|indigo|violet)""")

    const val SPRING = 118
    const val SPROCKET = 119
    const val COG = 120
    const val MINERS_HELMET = 360
    const val MINERS_PANTS = 361
    const val MATTOCK = 362
    const val LINOLEUM_ORE = 363
    const val ASBESTOS_ORE = 364
    const val CHROME_ORE = 365
    const val DWARF_BREAD = 910
    const val LUMP_OF_COAL = 3199
    const val SMALL_LAMINATED_CARD = 3208
    const val LITTLE_LAMINATED_CARD = 3209
    const val NOTBIG_LAMINATED_CARD = 3210
    const val UNLARGE_LAMINATED_CARD = 3211
    const val DWARVISH_DOCUMENT = 3212
    const val DWARVISH_PAPER = 3213
    const val DWARVISH_PARCHMENT = 3214

    private val ITEMS = intArrayOf(
        SPRING, SPROCKET, COG,
        MINERS_HELMET, MINERS_PANTS, MATTOCK,
        LINOLEUM_ORE, ASBESTOS_ORE, CHROME_ORE,
        DWARF_BREAD, LUMP_OF_COAL,
    )
    private val ORES = intArrayOf(LINOLEUM_ORE, ASBESTOS_ORE, CHROME_ORE, LUMP_OF_COAL)
    private val EQUIPMENT = intArrayOf(MINERS_HELMET, MINERS_PANTS, MATTOCK)

    private var digits: DwarfNumberTranslator? = null
    private val hopperCount = intArrayOf(-1, -1, -1, -1)

    fun reset() {
        digits = null
        hopperCount.fill(-1)
    }

    fun valid(): Boolean = digits?.valid() == true

    fun hopperCount(index: Int): Int =
        hopperCount.getOrElse(index) { -1 }

    fun ensureUpdated(preferences: Preferences, ascensions: Int) {
        val last = preferences.getInt("lastDwarfFactoryReset", -1)
        if (last >= ascensions) return
        preferences.setInt("lastDwarfFactoryReset", ascensions)
        preferences.setString("lastDwarfDiceRolls", "")
        preferences.setString("lastDwarfDigitRunes", "-------")
        preferences.setString("lastDwarfEquipmentRunes", "")
        preferences.setString("lastDwarfHopper1", "")
        preferences.setString("lastDwarfHopper2", "")
        preferences.setString("lastDwarfHopper3", "")
        preferences.setString("lastDwarfHopper4", "")
        for (id in ITEMS) preferences.setString("lastDwarfFactoryItem$id", "")
        for (id in 3208..3214) preferences.setString("lastDwarfOfficeItem$id", "")
        preferences.setString("lastDwarfOreRunes", "")
        reset()
    }

    fun clearHoppers() {
        hopperCount.fill(0)
    }

    fun parseResponse(
        urlString: String,
        responseText: String,
        preferences: Preferences,
        ascensions: Int = preferences.getInt("lastDwarfFactoryReset", 0).coerceAtLeast(0),
        sessionLogger: SessionLogger? = null,
    ) {
        if (!urlString.contains("dwarffactory.php", ignoreCase = true)) return
        val action = ACTION.find(urlString)?.groupValues?.get(1) ?: return
        when (action) {
            "ware" -> {
                val runes = RUNE.findAll(responseText).map { it.groupValues[2] }.toList()
                val rune1 = runes.getOrElse(0) { "" }
                val rune2 = runes.getOrElse(1) { "" }
                val rune3 = runes.getOrElse(2) { "" }
                val itemId = getItemId(responseText)
                setItemRunesFromWarehouse(itemId, rune1, rune2, rune3, preferences, ascensions)
            }
            "dodice" -> parseDice(responseText, preferences, ascensions, sessionLogger)
        }
    }

    private fun parseDice(
        responseText: String,
        preferences: Preferences,
        ascensions: Int,
        sessionLogger: SessionLogger?,
    ) {
        val meatMatch = MEAT.find(responseText) ?: return
        val won = meatMatch.groupValues[1] == "gain"
        val meat = (meatMatch.groupValues[2].toIntOrNull() ?: 0) / 7
        val meat7 = "${meat / 7}${meat % 7}"
        val runes = RUNE.findAll(responseText).map { it.groupValues[2] }.toList()
        if (runes.size < 4) return
        val first = runes[0] + runes[1]
        val second = runes[2] + runes[3]
        val message = if (won) {
            "$second-$first=$meat7"
        } else {
            "$first-$second=$meat7"
        }
        sessionLogger?.appendRawLine(message)
        addDieRoll(message, preferences, ascensions)
    }

    private fun setItemRunesFromWarehouse(
        itemId: Int,
        rune1: String,
        rune2: String,
        rune3: String,
        preferences: Preferences,
        ascensions: Int,
    ) {
        ensureUpdated(preferences, ascensions)
        if (itemId == -1) return
        var typeRunes = ""
        when (itemId) {
            LINOLEUM_ORE, ASBESTOS_ORE, CHROME_ORE, LUMP_OF_COAL -> {
                val ores = preferences.getString("lastDwarfOreRunes")
                if (ores.length == 4) typeRunes = ores
            }
            MINERS_HELMET, MINERS_PANTS, MATTOCK -> {
                val equipment = preferences.getString("lastDwarfEquipmentRunes")
                if (equipment.length == 3) typeRunes = equipment
            }
        }
        val setting = "lastDwarfFactoryItem$itemId"
        val oldRunes = preferences.getString(setting)
        var newRunes = ""
        for (rune in listOf(rune1, rune2, rune3)) {
            if (rune.isEmpty()) continue
            if (typeRunes.isEmpty() || typeRunes.indexOf(rune) != -1) {
                if (oldRunes.isEmpty() || oldRunes.indexOf(rune) != -1) {
                    newRunes += rune
                }
            }
        }
        for (id in ITEMS) {
            if (id == itemId) continue
            val value = preferences.getString("lastDwarfFactoryItem$id")
            if (value.length == 1 && newRunes.indexOf(value) != -1) {
                newRunes = newRunes.replace(value, "")
            }
        }
        setItemRunes(itemId, newRunes, preferences)
    }

    fun setItemRunes(itemId: Int, runes: String, preferences: Preferences) {
        preferences.setString("lastDwarfFactoryItem$itemId", runes)
        if (runes.length > 1) return
        when (itemId) {
            LINOLEUM_ORE, ASBESTOS_ORE, CHROME_ORE, LUMP_OF_COAL -> {
                val ores = preferences.getString("lastDwarfOreRunes")
                if (ores.length == 4) checkForLastRune(ores, ORES, preferences)
            }
            MINERS_HELMET, MINERS_PANTS, MATTOCK -> {
                val equipment = preferences.getString("lastDwarfEquipmentRunes")
                if (equipment.length == 3) checkForLastRune(equipment, EQUIPMENT, preferences)
            }
        }
        pruneItemRunes(itemId, runes, preferences)
    }

    private fun checkForLastRune(runes: String, items: IntArray, preferences: Preferences) {
        var candidate = 0
        var remaining = runes
        for (itemId in items) {
            val value = preferences.getString("lastDwarfFactoryItem$itemId")
            if (value.length != 1) {
                if (candidate != 0) return
                candidate = itemId
                continue
            }
            remaining = remaining.replace(value, "")
        }
        if (candidate != 0) setItemRunes(candidate, remaining, preferences)
    }

    private fun pruneItemRunes(id: Int, rune: String, preferences: Preferences) {
        for (itemId in ITEMS) {
            if (id == itemId) continue
            eliminateItemRune(itemId, rune, preferences)
        }
    }

    private fun eliminateItemRune(itemId: Int, rune: String, preferences: Preferences) {
        val setting = "lastDwarfFactoryItem$itemId"
        var value = preferences.getString(setting)
        if (value.length == 1 || value.indexOf(rune) == -1) return
        value = value.replace(rune, "")
        preferences.setString(setting, value)
        if (value.length == 1) pruneItemRunes(itemId, value, preferences)
    }

    fun setHopperRune(hopper: Int, responseText: String, preferences: Preferences, ascensions: Int) {
        ensureUpdated(preferences, ascensions)
        val rune = getRune(responseText)
        preferences.setString("lastDwarfHopper$hopper", rune)
        setOreRune(rune, preferences)
        if (responseText.contains("It is currently empty")) {
            hopperCount[hopper - 1] = 0
            return
        }
        val match = HOPPER.find(responseText) ?: return
        hopperCount[hopper - 1] = match.groupValues[1].toIntOrNull() ?: return
    }

    fun setOreRune(rune: String, preferences: Preferences) {
        if (rune.isEmpty()) return
        var runes = preferences.getString("lastDwarfOreRunes")
        if (runes.indexOf(rune) != -1) return
        runes += rune
        preferences.setString("lastDwarfOreRunes", runes)
        if (runes.length == 4) checkForLastRune(runes, ORES, preferences)
        for (itemId in ITEMS) {
            if (itemId in ORES) continue
            eliminateItemRune(itemId, rune, preferences)
        }
    }

    fun setEquipmentRune(rune: String, preferences: Preferences) {
        if (rune.isEmpty()) return
        var runes = preferences.getString("lastDwarfEquipmentRunes")
        if (runes.indexOf(rune) != -1) return
        runes += rune
        preferences.setString("lastDwarfEquipmentRunes", runes)
        if (runes.length == 3) checkForLastRune(runes, EQUIPMENT, preferences)
        for (itemId in ITEMS) {
            if (itemId in EQUIPMENT) continue
            eliminateItemRune(itemId, rune, preferences)
        }
    }

    fun getRune(responseText: String): String =
        RUNE.find(responseText)?.groupValues?.get(2).orEmpty()

    fun getRunes(responseText: String): String =
        RUNE.findAll(responseText).joinToString("") { it.groupValues[2] }

    fun setDigits(digitString: String, preferences: Preferences) {
        preferences.setString("lastDwarfDigitRunes", digitString)
        digits = DwarfNumberTranslator(digitString)
    }

    fun parseNumber(runes: String, preferences: Preferences? = null): Int {
        val translator = digits ?: DwarfNumberTranslator(
            preferences?.getString("lastDwarfDigitRunes") ?: "-------",
        ).also { digits = it }
        return translator.parseNumber(runes)
    }

    fun parseNumber(runes: String, digitString: String): Int =
        DwarfNumberTranslator(digitString).parseNumber(runes)

    fun useUnlaminatedItem(itemId: Int, responseText: String, preferences: Preferences) {
        val builder = StringBuilder()
        var count = 0
        for (match in RUNE.findAll(responseText)) {
            val rune = match.groupValues[2]
            val type = match.groupValues[1]
            if (count++ == 0) {
                setEquipmentRune(rune, preferences)
                builder.append(rune)
                continue
            }
            if (type == "Word") {
                setOreRune(rune, preferences)
                builder.append(',')
            }
            builder.append(rune)
        }
        preferences.setString("lastDwarfOfficeItem$itemId", builder.toString())
    }

    fun useLaminatedItem(itemId: Int, responseText: String, preferences: Preferences) {
        val builder = StringBuilder()
        var count = 0
        for (match in RUNE.findAll(responseText)) {
            val rune = match.groupValues[2]
            val type = match.groupValues[1]
            if (count++ == 0) {
                setOreRune(rune, preferences)
                builder.append(rune)
                continue
            }
            if (count == 2) continue
            if (type == "Word") {
                setEquipmentRune(rune, preferences)
                builder.append(',')
            }
            builder.append(rune)
        }
        preferences.setString("lastDwarfOfficeItem$itemId", builder.toString())
    }

    private fun getItemId(responseText: String): Int {
        val descId = ITEMDESC.find(responseText)?.groupValues?.get(1) ?: return -1
        return ItemDatabase.getByDescId(descId)?.id ?: -1
    }

    fun deduceHP(responseText: CharSequence): Int {
        val match = MATTOCK_PATTERN.find(responseText) ?: return 0
        return match.groupValues[1].length
    }

    fun deduceAttack(responseText: CharSequence, preferences: Preferences? = null): Int {
        val match = HELMET.find(responseText) ?: return 0
        return parseNumber(getRunes(match.value), preferences)
    }

    fun deduceDefense(responseText: CharSequence): Int {
        val match = KILT.find(responseText) ?: return 0
        if (match.value.contains("rave on your crotch")) return 99999
        var number = 0
        for (colorMatch in COLOR.findAll(match.value)) {
            val digit = when (colorMatch.groupValues[1]) {
                "red" -> 0
                "orange" -> 1
                "yellow" -> 2
                "green" -> 3
                "blue" -> 4
                "indigo" -> 5
                "violet" -> 6
                else -> return -1
            }
            number = number * 7 + digit
        }
        return number
    }

    fun check(
        preferences: Preferences,
        print: (String) -> Unit = {},
    ): Boolean {
        val missing = mutableListOf<String>()
        for (itemId in EQUIPMENT + ORES) {
            if (preferences.getString("lastDwarfFactoryItem$itemId").length != 1) {
                val name = ItemDatabase.getById(itemId)?.name ?: "item $itemId"
                missing += "You not yet identified the $name"
            }
        }
        if (missing.isEmpty()) return true
        missing.forEach(print)
        return false
    }

    fun solve(preferences: Preferences, print: (String) -> Unit = {}): Boolean {
        val translator = digits ?: DwarfNumberTranslator(
            preferences.getString("lastDwarfDigitRunes"),
        ).also { digits = it }
        if (translator.valid()) return true
        for (s in getUnlaminatedNumbers(preferences)) translator.addNumber(s)
        for (s in getLaminatedNumbers(preferences)) translator.addNumber(s)
        translator.analyzeNumbers()
        for (roll in preferences.getString("lastDwarfDiceRolls").split(':')) {
            if (roll.isNotEmpty()) translator.addRoll(roll)
        }
        translator.analyzeRolls()
        preferences.setString("lastDwarfDigitRunes", translator.digitString())
        return if (translator.valid()) {
            print("Dwarf digit code solved!")
            true
        } else {
            print("Unable to solve digit code. Get more data!")
            false
        }
    }

    fun report(preferences: Preferences, print: (String) -> Unit) {
        solve(preferences, print)
        val translator = digits ?: return
        if (!translator.valid()) {
            print("Invalid or incomplete digit set")
            return
        }
        if (!check(preferences, print)) return
        for (i in 1..4) {
            val rune = preferences.getString("lastDwarfHopper$i")
            val count = hopperCount(i - 1)
            print("Hopper #$i rune=$rune count=$count")
        }
        for (itemId in EQUIPMENT + ORES) {
            val rune = preferences.getString("lastDwarfFactoryItem$itemId")
            val name = ItemDatabase.getById(itemId)?.name ?: "item $itemId"
            print("$name = $rune")
        }
        print("Digits: ${translator.digitString()}")
    }

    fun report(digitString: String, preferences: Preferences, print: (String) -> Unit) {
        if (digitString.length != 7) {
            print("Digit string must have 7 characters")
            return
        }
        setDigits(digitString, preferences)
        report(preferences, print)
    }

    private fun addDieRoll(message: String, preferences: Preferences, ascensions: Int) {
        ensureUpdated(preferences, ascensions)
        val dice = preferences.getString("lastDwarfDiceRolls")
        preferences.setString("lastDwarfDiceRolls", "$dice$message:")
        solve(preferences)
    }

    private fun getUnlaminatedNumbers(preferences: Preferences): List<String> {
        val numbers = mutableListOf<String>()
        getItemNumbers(numbers, DWARVISH_DOCUMENT, 4, preferences)
        getItemNumbers(numbers, DWARVISH_PAPER, 4, preferences)
        getItemNumbers(numbers, DWARVISH_PARCHMENT, 4, preferences)
        return numbers
    }

    private fun getLaminatedNumbers(preferences: Preferences): List<String> {
        val numbers = mutableListOf<String>()
        getItemNumbers(numbers, SMALL_LAMINATED_CARD, 3, preferences)
        getItemNumbers(numbers, LITTLE_LAMINATED_CARD, 3, preferences)
        getItemNumbers(numbers, NOTBIG_LAMINATED_CARD, 3, preferences)
        getItemNumbers(numbers, UNLARGE_LAMINATED_CARD, 3, preferences)
        return numbers
    }

    private fun getItemNumbers(
        list: MutableList<String>,
        itemId: Int,
        count: Int,
        preferences: Preferences,
    ) {
        val setting = preferences.getString("lastDwarfOfficeItem$itemId")
        val splits = setting.split(',')
        if (splits.size != count + 1) return
        for (i in 1..count) {
            if (splits[i].length > 1) list.add(splits[i].substring(1))
        }
    }

    fun registerRequest(urlString: String, sessionLogger: SessionLogger?): Boolean {
        if (!urlString.contains("dwarffactory.php", ignoreCase = true)) return false
        val action = ACTION.find(urlString)?.groupValues?.get(1) ?: return true
        when (action) {
            "ware" -> {
                sessionLogger?.appendRawLine("Dwarven Factory Warehouse")
                return true
            }
            "dorm" -> {
                sessionLogger?.appendRawLine("Visiting the Dwarven Factory Dormitory")
                return true
            }
            "dodice", "nodice", "nonodice" -> return true
        }
        return false
    }

    fun resetForTest() {
        reset()
    }
}
