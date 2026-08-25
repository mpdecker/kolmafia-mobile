package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BreakfastItemIds

/** Desktop GenieRequest — inv_use bottle/pocket wish → choice 1267. */
class GenieRequest(
    private val useItemRequest: UseItemRequest,
    private val choiceRequest: ChoiceRequest,
    private val inventoryManager: InventoryManager? = null,
) {
    suspend fun makeWish(
        wish: String,
        preferences: Preferences?,
        inventoryCounts: (Int) -> Int,
        inLegacyOfLoathing: Boolean = false,
    ): Result<String> {
        if (RequestAbortGate.abortIfInFightOrChoice()) {
            return Result.failure(IllegalStateException(RequestAbortGate.lastAbortMessage.ifEmpty {
                "You are currently in a fight or choice."
            }))
        }
        val normalizedWish = wish.trim()
        if (normalizedWish.isEmpty()) {
            return Result.failure(IllegalArgumentException("No wish specified."))
        }
        if (isCombatWish(normalizedWish)) {
            return Result.failure(
                IllegalArgumentException("Combat genie wishes are not supported yet."),
            )
        }
        val pocketGuard = preflightPocketMoreWishes(
            normalizedWish,
            preferences,
            inventoryCounts,
            inLegacyOfLoathing,
        )
        if (pocketGuard != null) {
            return Result.failure(IllegalStateException(pocketGuard))
        }
        val itemId = selectWishItem(preferences, inventoryCounts, inLegacyOfLoathing)
            ?: return Result.failure(
                IllegalStateException("You do not have a genie bottle or pocket wish to use."),
            )
        val useResult = useItemRequest.use(itemId, 1)
        useResult.onFailure { return Result.failure(it) }
        useResult.onSuccess { html -> visitChoice(html, preferences) }

        val choiceResult = choiceRequest.choose(
            CHOICE_ID,
            1,
            mapOf("wish" to normalizedWish),
        )
        return choiceResult.map { (html, _) ->
            postChoice(
                html = html,
                wish = normalizedWish,
                preferences = preferences,
                usedPocketWish = itemId == POCKET_WISH_ID,
                inventoryManager = inventoryManager,
            )
            html
        }
    }

    companion object {
        const val CHOICE_ID = 1267
        const val WISHES_USED_PREF = "_genieWishesUsed"
        const val POCKET_WISH_ID = 9537
        private val WISH_LEFT_PATTERN = Regex("""You have (\d) wish""", RegexOption.IGNORE_CASE)

        fun resolveWish(parameters: String): Result<String> {
            val trimmed = parameters.trim()
            if (trimmed.isEmpty()) {
                return Result.failure(IllegalArgumentException("Usage: genie effect|meat|stat|item|wish …"))
            }
            val lower = trimmed.lowercase()
            return when {
                lower.startsWith("wish ") -> Result.success(trimmed.substring(5).trim())
                lower == "meat" -> Result.success("I was rich")
                lower.startsWith("item ") -> resolveItemWish(trimmed.substring(5).trim())
                lower.startsWith("stat ") -> resolveStatWish(trimmed.substring(5).trim())
                lower.startsWith("effect ") -> resolveEffectWish(trimmed.substring(7).trim())
                lower.startsWith("monster ") || lower == "freedom" ->
                    Result.failure(IllegalArgumentException("Combat genie wishes are not supported yet."))
                else -> Result.failure(
                    IllegalArgumentException(
                        "Usage: genie effect <name> | meat | stat mus|mys|mox|all | item pony|pocket|shirt | wish <text>",
                    ),
                )
            }
        }

        private fun resolveItemWish(arg: String): Result<String> {
            val a = arg.lowercase()
            return when {
                a.startsWith("pocket") -> Result.success("for more wishes")
                a.startsWith("pony") -> Result.success("for a pony")
                a.startsWith("shirt") ->
                    Result.success("for a blessed rustproof +2 gray dragon scale mail")
                else -> Result.failure(IllegalArgumentException("Unknown genie item wish."))
            }
        }

        private fun resolveStatWish(arg: String): Result<String> {
            val a = arg.lowercase()
            return when {
                a.startsWith("mus") -> Result.success("I was a little bit taller")
                a.startsWith("mys") -> Result.success("I had a rabbit in a hat with a bat")
                a.startsWith("mox") -> Result.success("I was a baller")
                a.startsWith("all") -> Result.success("I was big")
                else -> Result.failure(IllegalArgumentException("Unknown genie stat wish."))
            }
        }

        private fun resolveEffectWish(name: String): Result<String> {
            val effect = EffectDatabase.getByName(name)
                ?: return Result.failure(
                    IllegalArgumentException("$name does not match exactly one effect"),
                )
            return Result.success("to be ${effect.name}")
        }

        fun selectWishItem(
            preferences: Preferences?,
            inventoryCounts: (Int) -> Int,
            inLegacyOfLoathing: Boolean,
        ): Int? {
            val used = preferences?.getInt(WISHES_USED_PREF, 0) ?: 0
            if (inventoryCounts(BreakfastItemIds.GENIE_BOTTLE_ID) > 0 && used < 3) {
                return BreakfastItemIds.GENIE_BOTTLE_ID
            }
            if (inLegacyOfLoathing &&
                inventoryCounts(BreakfastItemIds.REPLICA_GENIE_BOTTLE_ID) > 0 &&
                used < 3
            ) {
                return BreakfastItemIds.REPLICA_GENIE_BOTTLE_ID
            }
            if (inventoryCounts(POCKET_WISH_ID) > 0) return POCKET_WISH_ID
            return null
        }

        fun isCombatWish(wish: String): Boolean {
            val w = wish.lowercase().trim()
            return w == "you were free" ||
                w.startsWith("to fight") ||
                w.startsWith("to be fighting a")
        }

        fun visitChoice(html: String, preferences: Preferences?) {
            if (preferences == null) return
            val match = WISH_LEFT_PATTERN.find(html)
            if (match != null) {
                val left = match.groupValues[1].toIntOrNull() ?: return
                preferences.setInt(WISHES_USED_PREF, 3 - left)
            }
        }

        fun postChoice(
            html: String,
            wish: String,
            preferences: Preferences?,
            usedPocketWish: Boolean,
            inventoryManager: InventoryManager? = null,
        ) {
            val success = html.contains("You acquire") ||
                html.contains("You gain") ||
                html.contains(">Fight!<")
            if (!success) return
            if (usedPocketWish) {
                inventoryManager?.consumeItemLocally(POCKET_WISH_ID, 1)
            } else if (preferences != null) {
                preferences.setInt(
                    WISHES_USED_PREF,
                    preferences.getInt(WISHES_USED_PREF, 0) + 1,
                )
            }
        }

        fun preflightPocketMoreWishes(
            wish: String,
            preferences: Preferences?,
            inventoryCounts: (Int) -> Int,
            inLegacyOfLoathing: Boolean,
        ): String? {
            if (!wish.equals("for more wishes", ignoreCase = true)) return null
            val used = preferences?.getInt(WISHES_USED_PREF, 0) ?: 0
            val hasBottle = inventoryCounts(BreakfastItemIds.GENIE_BOTTLE_ID) > 0 ||
                (inLegacyOfLoathing && inventoryCounts(BreakfastItemIds.REPLICA_GENIE_BOTTLE_ID) > 0)
            if (used >= 3 || !hasBottle) {
                return "Don't use a pocket wish to make a pocket wish."
            }
            return null
        }
    }
}
