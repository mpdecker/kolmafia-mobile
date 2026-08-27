package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.EquipmentDiscard

/**
 * Desktop [FightRequest.updateFinalRoundData] high-traffic subset (Phases 1286–1300)
 * plus clearing [EncounterManager.ignoreSpecialMonsters] at fight end.
 */
object FightFinalRoundSync {

    const val SUGAR_CHAPEAU = 4181
    const val SUGAR_SHANK = 4182
    const val SUGAR_SHIELD = 4183
    const val SUGAR_SHILLELAGH = 4184
    const val SUGAR_SHIRT = 4185
    const val SUGAR_SHOTGUN = 4186
    const val SUGAR_SHORTS = 4187
    const val BROKEN_CHAMPAGNE = 10636
    const val MAKESHIFT_GARBAGE_SHIRT = 10635
    const val PANTSGIVING = 6860
    const val DECEASED_TREE = 10634

    private val GARBAGE_SHIRT = Regex(
        """Looks like there are ([\d,]+) more useful scraps""",
    )
    private val CHAMPAGNE = Regex(
        """(\d+) ounces of champagne left""",
    )
    private val TREE_NEEDLES = Regex(
        """Your crimbo tree has ([\d,]+) needles left""",
    )

    /**
     * Apply end-of-fight / per-round final data. Call when a fight round completes;
     * [fightEnded] true when leaving multi-fight (desktop clearFight window).
     */
    fun apply(
        html: String,
        preferences: Preferences?,
        character: KoLCharacter? = null,
        inventory: InventoryManager? = null,
        won: Boolean = false,
        lost: Boolean = false,
        fightEnded: Boolean = false,
        garbledCombat: Boolean = false,
        stinkyCheeseLevel: Int = 0,
        familiarId: Int = 0,
        adventureId: Int = -1,
        underwater: Boolean = false,
    ): Boolean {
        if (preferences == null) return false
        var changed = false
        changed = applyGhostPepper(html, preferences, character, garbledCombat) || changed
        changed = applyGetsYouDrunk(html, preferences, character, garbledCombat) || changed
        if (stinkyCheeseLevel > 0) {
            preferences.setInt(
                "_stinkyCheeseCount",
                preferences.getInt("_stinkyCheeseCount", 0) + stinkyCheeseLevel,
            )
            changed = true
        }
        if (hasEquipped(character, PANTSGIVING)) {
            preferences.setInt(
                "_pantsgivingCount",
                preferences.getInt("_pantsgivingCount", 0) + 1,
            )
            changed = true
        }
        changed = applySugarBreaks(html, character, inventory) || changed
        changed = applyGarbageCharges(html, preferences, character, inventory) || changed
        changed = applyUnicornHorn(html, preferences) || changed
        val resolvedFamiliarId = if (familiarId != 0) {
            familiarId
        } else {
            character?.state?.value?.familiarId ?: 0
        }
        changed = FightFamiliarProgressSync.apply(
            html = html,
            preferences = preferences,
            familiarId = resolvedFamiliarId,
            won = won,
            fightEnded = fightEnded,
            underwater = underwater,
            adventureId = adventureId,
        ) || changed

        if (fightEnded) {
            EncounterManager.ignoreSpecialMonsters = false
            FightActionCostSync.reset()
            changed = true
        }
        return changed
    }

    fun applyGhostPepper(
        html: String,
        preferences: Preferences,
        character: KoLCharacter?,
        garbledCombat: Boolean,
    ): Boolean {
        val alive = (character?.state?.value?.currentHp ?: 1) > 0
        return when {
            html.contains("The ghost pepper you ate") && alive -> {
                preferences.setInt(
                    "ghostPepperTurnsLeft",
                    (preferences.getInt("ghostPepperTurnsLeft", 0) - 1).coerceAtLeast(1),
                )
                true
            }
            garbledCombat -> {
                preferences.setInt(
                    "ghostPepperTurnsLeft",
                    (preferences.getInt("ghostPepperTurnsLeft", 0) - 1).coerceAtLeast(0),
                )
                true
            }
            preferences.getInt("ghostPepperTurnsLeft", 0) != 0 -> {
                preferences.setInt("ghostPepperTurnsLeft", 0)
                true
            }
            else -> false
        }
    }

    fun applyGetsYouDrunk(
        html: String,
        preferences: Preferences,
        character: KoLCharacter?,
        garbledCombat: Boolean,
    ): Boolean {
        val alive = (character?.state?.value?.currentHp ?: 1) > 0
        return when {
            html.contains("The mulled wine you drank") && alive -> {
                preferences.setInt(
                    "getsYouDrunkTurnsLeft",
                    (preferences.getInt("getsYouDrunkTurnsLeft", 0) - 1).coerceAtLeast(1),
                )
                true
            }
            garbledCombat -> {
                preferences.setInt(
                    "getsYouDrunkTurnsLeft",
                    (preferences.getInt("getsYouDrunkTurnsLeft", 0) - 1).coerceAtLeast(0),
                )
                true
            }
            preferences.getInt("getsYouDrunkTurnsLeft", 0) != 0 -> {
                preferences.setInt("getsYouDrunkTurnsLeft", 0)
                true
            }
            else -> false
        }
    }

    fun applySugarBreaks(
        html: String,
        character: KoLCharacter?,
        inventory: InventoryManager?,
    ): Boolean {
        if (character == null) return false
        var changed = false
        fun breakItem(itemId: Int, phrase: String) {
            if (html.contains(phrase)) {
                changed = EquipmentDiscard.discardIfEquipped(
                    itemId = itemId,
                    equipment = character.state.value.equipment,
                    clearSlot = { slot -> character.updateEquipment(slot, "") },
                    consumeItem = { id, qty -> inventory?.consumeItemLocally(id, qty) },
                ) || changed
            }
        }
        breakItem(SUGAR_CHAPEAU, "Your sugar chapeau slides")
        breakItem(SUGAR_SHANK, "your sugar shank handle")
        breakItem(SUGAR_SHIELD, "drop something as sticky as the sugar shield")
        breakItem(SUGAR_SHILLELAGH, "Your sugar shillelagh absorbs the shock")
        breakItem(SUGAR_SHIRT, "Your sugar shirt falls apart")
        breakItem(SUGAR_SHOTGUN, "Your sugar shotgun falls apart")
        breakItem(SUGAR_SHORTS, "Your sugar shorts crack")
        return changed
    }

    fun applyGarbageCharges(
        html: String,
        preferences: Preferences,
        character: KoLCharacter?,
        inventory: InventoryManager?,
    ): Boolean {
        var changed = false
        TREE_NEEDLES.find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()?.let {
            preferences.setInt("garbageTreeCharge", it)
            changed = true
        }
        if (html.contains("Your crimbo tree is now 100% naked")) {
            preferences.setInt("garbageTreeCharge", 0)
            if (character != null) {
                EquipmentDiscard.discardIfEquipped(
                    itemId = DECEASED_TREE,
                    equipment = character.state.value.equipment,
                    clearSlot = { slot -> character.updateEquipment(slot, "") },
                    consumeItem = { id, qty -> inventory?.consumeItemLocally(id, qty) },
                )
            }
            changed = true
        }
        if (html.contains("champagne is flowing and the party is going wild")) {
            CHAMPAGNE.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
                preferences.setInt("garbageChampagneCharge", it)
                changed = true
            }
        } else if (html.contains("last drop of your party champagne dripped out")) {
            preferences.setInt("garbageChampagneCharge", 0)
            if (character != null) {
                EquipmentDiscard.discardIfEquipped(
                    itemId = BROKEN_CHAMPAGNE,
                    equipment = character.state.value.equipment,
                    clearSlot = { slot -> character.updateEquipment(slot, "") },
                    consumeItem = { id, qty -> inventory?.consumeItemLocally(id, qty) },
                )
            }
            changed = true
        }
        if (html.contains("read a useful bit of information off your shirt")) {
            GARBAGE_SHIRT.find(html)?.groupValues?.getOrNull(1)?.replace(",", "")
                ?.toIntOrNull()?.let {
                    preferences.setInt("garbageShirtCharge", it)
                    changed = true
                }
        } else if (html.contains("last bit of usefully informative garbage off your shirt")) {
            preferences.setInt("garbageShirtCharge", 0)
            if (character != null) {
                EquipmentDiscard.discardIfEquipped(
                    itemId = MAKESHIFT_GARBAGE_SHIRT,
                    equipment = character.state.value.equipment,
                    clearSlot = { slot -> character.updateEquipment(slot, "") },
                    consumeItem = { id, qty -> inventory?.consumeItemLocally(id, qty) },
                )
            }
            changed = true
        }
        return changed
    }

    fun applyUnicornHorn(html: String, preferences: Preferences): Boolean = when {
        html.contains("your unicorn horn begins to inflate") -> {
            preferences.setInt("unicornHornInflation", 5)
            true
        }
        html.contains("Your unicorn horn becomes slightly more inflated") -> {
            preferences.setInt(
                "unicornHornInflation",
                preferences.getInt("unicornHornInflation", 0) + 1,
            )
            true
        }
        else -> false
    }

    private fun hasEquipped(character: KoLCharacter?, itemId: Int): Boolean {
        if (character == null) return false
        // Match by known pantsgiving name substring when ItemDatabase unavailable
        val pants = character.state.value.equipment[EquipmentSlot.PANTS].orEmpty()
        return pants.contains("pantsgiving", ignoreCase = true) ||
            (itemId == PANTSGIVING && pants.isNotEmpty() && pants.contains("giving", ignoreCase = true))
    }
}
