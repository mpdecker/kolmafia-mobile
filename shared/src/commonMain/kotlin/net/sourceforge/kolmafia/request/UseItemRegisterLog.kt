package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.ZodiacSign

/**
 * Desktop [UseItemRequest.registerRequest] session-log lines for inv_use
 * (Phases 8111–8170). Pref writes stay in [UseItemRequestState].
 *
 * [Outcome.Skip] is a desktop `return true` that recognizes the URL and writes
 * no line (fold picker, trivia question, seal confirm). [Outcome.Fallback] keeps
 * the existing `use N item` line.
 */
object UseItemRegisterLog {

    sealed class Outcome {
        data object Skip : Outcome()
        data object Fallback : Outcome()
        data class Line(val text: String, val markLocationLogged: Boolean = false) : Outcome()
    }

    data class EquipLine(
        val text: String,
        val slot: String,
        val newItemId: Int,
        val discardPrevious: Boolean = false,
    )

    fun describe(
        url: String,
        itemId: Int,
        count: Int,
        itemName: (Int) -> String,
        adventureCount: Int,
        familiarId: Int,
    ): Outcome {
        val qty = count.coerceAtLeast(1)
        val name = label(itemName, itemId)
        if ((itemId in SEALS || itemId in BRICKO) && !url.contains("checked=1")) {
            return Outcome.Skip
        }
        return when (itemId) {
            REFLECTION_OF_MAP -> Outcome.Line("[$adventureCount] Reflection of a Map", markLocationLogged = true)
            DREADSCROLL -> Outcome.Line("[$adventureCount] Mer-kin dreadscroll", markLocationLogged = true)
            JACKING_MAP -> jacking(url, itemName)
            EL_VIBRATO_HELMET, DRONE -> helperInsert(url, name, itemName)
            in PUNCHCARDS ->
                if (familiarId == MEGADRONE) Outcome.Line("insert $name into El Vibrato Megadrone")
                else Outcome.Skip
            in SEALS -> if (url.contains("checked")) Outcome.Skip else Outcome.Fallback
            in LEGION_FOLD ->
                if (url.contains("fold")) Outcome.Line("fold $name") else Outcome.Skip
            SCREWDRIVER -> screwdriver(url, name, itemName)
            TATTOO_NEEDLE ->
                if (url.contains("switch")) {
                    if (url.contains("fold")) Outcome.Line("fold $name") else Outcome.Skip
                } else {
                    Outcome.Fallback
                }
            D10 ->
                if (qty == 2) Outcome.Line("roll percentile dice")
                else Outcome.Line("roll $qty$name")
            D4, D6, D8, D12, D20 -> Outcome.Line("roll $qty$name")
            in TRIVIA -> if (url.contains("answerplz=1")) Outcome.Fallback else Outcome.Skip
            in BARRELS ->
                if (url.contains("choice=1")) Outcome.Line("Throw a barrel smashing party!")
                else Outcome.Line("smash $name")
            in BEANS -> Outcome.Line("plate $name")
            HEWN_SPOON, REPLICA_SPOON -> moon(url)
            else -> Outcome.Fallback
        }
    }

    fun equipped(url: String, equippedId: (String) -> Int, itemName: (Int) -> String): EquipLine? {
        if (url.contains("action=twisthorns")) {
            val slot = when {
                url.contains("slot=hat") -> "hat"
                url.contains("slot=familiarequip") -> "familiarequip"
                else -> return null
            }
            val before = equippedId(slot)
            val after = if (before == BORIS_HELM) BORIS_HELM_ASKEW else BORIS_HELM
            return EquipLine(
                "Twisted ${label(itemName, before)} into ${label(itemName, after)}",
                slot,
                after,
            )
        }
        if (url.contains("action=shakepan")) {
            val before = equippedId("offhand")
            val after = if (before == JARLS_PAN) JARLS_COSMIC_PAN else JARLS_PAN
            return EquipLine(
                "Shook ${label(itemName, before)} into ${label(itemName, after)}",
                "offhand",
                after,
            )
        }
        if (url.contains("action=popcollar")) {
            val before = equippedId("shirt")
            val after = if (before == PETE_JACKET) PETE_JACKET_COLLAR else PETE_JACKET
            return EquipLine(
                "Popped ${label(itemName, before)} into ${label(itemName, after)}",
                "shirt",
                after,
            )
        }
        if (url.contains("action=togglebutt")) {
            val before = equippedId("familiarequip")
            val after = if (before == TOGGLE_BARTEND) TOGGLE_BOUNCE else TOGGLE_BARTEND
            return EquipLine(
                "Toggled ${label(itemName, before)} into ${label(itemName, after)}",
                "familiarequip",
                after,
                discardPrevious = true,
            )
        }
        return null
    }

    private fun jacking(url: String, itemName: (Int) -> String): Outcome {
        if (!url.contains("action=addfruit")) return Outcome.Fallback
        val fruit = WHICHFRUIT.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return Outcome.Fallback
        return Outcome.Line("insert ${label(itemName, fruit)} into pneumatic tube interface")
    }

    private fun helperInsert(url: String, item: String, itemName: (Int) -> String): Outcome {
        val helperId = HELPER.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return Outcome.Skip
        val helper = itemName(helperId)
        if (helper.isBlank()) return Outcome.Skip
        return Outcome.Line("insert $helper into $item")
    }

    private fun screwdriver(url: String, name: String, itemName: (Int) -> String): Outcome {
        if (url.contains("action=screw")) {
            val target = DOWHICH.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return Outcome.Skip
            val countStr = if (url.contains("untinkerall=on")) "*" else "1"
            return Outcome.Line("unscrew $countStr ${label(itemName, target)}")
        }
        return if (url.contains("fold")) Outcome.Line("fold $name") else Outcome.Skip
    }

    private fun moon(url: String): Outcome {
        if (!url.contains("doit=96")) return Outcome.Fallback
        val signId = WHICHSIGN.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return Outcome.Fallback
        val sign = ZodiacSign.find(signId) ?: return Outcome.Fallback
        return Outcome.Line("tuning moon to The ${sign.signName}")
    }

    private fun label(itemName: (Int) -> String, itemId: Int): String {
        val name = itemName(itemId)
        return name.ifBlank { "item #$itemId" }
    }

    private val HELPER = Regex("""(?:utensil|whichcard)=(\d+)""", RegexOption.IGNORE_CASE)
    private val WHICHFRUIT = Regex("""whichfruit=(\d+)""", RegexOption.IGNORE_CASE)
    private val DOWHICH = Regex("""dowhichitem=(\d+)""", RegexOption.IGNORE_CASE)
    private val WHICHSIGN = Regex("""whichsign=(\d+)""", RegexOption.IGNORE_CASE)

    private val PUNCHCARDS = 3146..3150
    private val SEALS = 3902..3911
    private val BRICKO = 4474..4484
    private val LEGION_FOLD = setOf(
        4908, 4909, 4910, 4911, 4912, 4913, 4914, 4915, 4916, 4917,
        4919, 4920, 4921, 4922, 4923, 4924, 4925, 4927, 4928,
    )
    private val TRIVIA = 5511..5514
    private val BARRELS = 8568..8577
    private val BEANS = 8866..8873

    private const val MEGADRONE = 81
    private const val DRONE = 3157
    private const val EL_VIBRATO_HELMET = 3162
    private const val REFLECTION_OF_MAP = 4509
    private const val JACKING_MAP = 4560
    private const val TATTOO_NEEDLE = 4918
    private const val SCREWDRIVER = 4926
    private const val D4 = 5285
    private const val D6 = 5286
    private const val D8 = 5287
    private const val D10 = 5288
    private const val D12 = 5289
    private const val D20 = 5290
    private const val BORIS_HELM = 5648
    private const val BORIS_HELM_ASKEW = 5650
    private const val DREADSCROLL = 6353
    private const val JARLS_COSMIC_PAN = 6304
    private const val JARLS_PAN = 6305
    private const val PETE_JACKET = 7250
    private const val PETE_JACKET_COLLAR = 7267
    private const val TOGGLE_BARTEND = 9402
    private const val TOGGLE_BOUNCE = 9403
    private const val HEWN_SPOON = 10254
    private const val REPLICA_SPOON = 11242
}
