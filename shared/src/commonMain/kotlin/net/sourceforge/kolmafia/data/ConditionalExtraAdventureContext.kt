package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.ash.currentDateString
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.preferences.Preferences

object ConditionalExtraAdventureItems {
    const val TUXEDO_SHIRT = 2489
    const val MAFIA_PINKY_RING = 9546
    const val FIELD_GAR_POTION = 5257
}

object ConditionalExtraAdventureEffects {
    const val REFINED_PALATE = "Refined Palate"
    const val GARISH = "Gar-ish"
}

object ConditionalExtraAdventureSkills {
    const val CLIP_ART = "Clip Art"
    const val PIZZA_LOVER = "Pizza Lover"
    const val BEANWEAVER = "Beanweaver"
    const val SAUCEMAVEN = "Saucemaven"
}

data class ConditionalExtraAdventureContext(
    val equippedItemIds: Set<Int> = emptySet(),
    val equippedItemNames: Set<String> = emptySet(),
    val activeEffectNames: List<String> = emptyList(),
    val skillNames: Set<String> = emptySet(),
    val autoTuxedo: Boolean = false,
    val autoPinkyRing: Boolean = false,
    val autoGarish: Boolean = false,
    val bondMartiniTurn: Boolean = false,
    val bondMartiniPlus: Boolean = false,
    val inBondcore: Boolean = false,
    val isMysticalityClass: Boolean = false,
    val isMonday: Boolean = false,
    val itemAvailable: (Int) -> Boolean = { false },
    val canEquip: (Int) -> Boolean = { false },
    val itemImage: (String) -> String = { name -> ItemDatabase.getByName(name)?.image ?: "" },
) {
    private val normalizedEffects = activeEffectNames.map { it.lowercase() }
    private val normalizedSkills = skillNames.map { it.lowercase() }.toSet()
    private val normalizedEquippedNames = equippedItemNames.map { it.lowercase() }.toSet()

    fun hasEffect(name: String): Boolean =
        normalizedEffects.any { it == name.lowercase() }

    fun hasSkill(name: String): Boolean =
        name.lowercase() in normalizedSkills

    fun hasEquipped(itemId: Int, itemName: String): Boolean {
        if (itemId in equippedItemIds) return true
        return itemName.lowercase() in normalizedEquippedNames
    }

    fun tuxedoAccessible(): Boolean =
        hasEquipped(ConditionalExtraAdventureItems.TUXEDO_SHIRT, "tuxedo shirt") ||
            (autoTuxedo &&
                canEquip(ConditionalExtraAdventureItems.TUXEDO_SHIRT) &&
                itemAvailable(ConditionalExtraAdventureItems.TUXEDO_SHIRT))

    fun pinkyRingAccessible(): Boolean =
        hasEquipped(ConditionalExtraAdventureItems.MAFIA_PINKY_RING, "mafia pinky ring") ||
            (autoPinkyRing &&
                canEquip(ConditionalExtraAdventureItems.MAFIA_PINKY_RING) &&
                itemAvailable(ConditionalExtraAdventureItems.MAFIA_PINKY_RING))

    fun garishAccessible(): Boolean =
        hasEffect(ConditionalExtraAdventureEffects.GARISH) ||
            (autoGarish &&
                (hasSkill(ConditionalExtraAdventureSkills.CLIP_ART) ||
                    itemAvailable(ConditionalExtraAdventureItems.FIELD_GAR_POTION)))

    companion object {
        val EMPTY = ConditionalExtraAdventureContext()
    }
}

fun buildConditionalExtraAdventureContext(
    preferences: Preferences? = null,
    activeEffectNames: List<String> = emptyList(),
    skillNames: Set<String> = emptySet(),
    ascensionPath: AscensionPath? = null,
    isMysticalityClass: Boolean = false,
    equippedItemNames: Set<String> = emptySet(),
    equippedItemIds: Set<Int> = emptySet(),
    dateYmd: String = currentDateString(),
    itemAvailable: (Int) -> Boolean = { false },
    canEquip: (Int) -> Boolean = { false },
    itemImage: (String) -> String = { name -> ItemDatabase.getByName(name)?.image ?: "" },
): ConditionalExtraAdventureContext = ConditionalExtraAdventureContext(
    equippedItemIds = equippedItemIds,
    equippedItemNames = equippedItemNames,
    activeEffectNames = activeEffectNames,
    skillNames = skillNames,
    autoTuxedo = preferences?.getBoolean("autoTuxedo") ?: false,
    autoPinkyRing = preferences?.getBoolean("autoPinkyRing") ?: false,
    autoGarish = preferences?.getBoolean("autoGarish") ?: false,
    bondMartiniTurn = preferences?.getBoolean("bondMartiniTurn") ?: false,
    bondMartiniPlus = preferences?.getBoolean("bondMartiniPlus") ?: false,
    inBondcore = ascensionPath == AscensionPath.LICENSE_TO_ADVENTURE,
    isMysticalityClass = isMysticalityClass,
    isMonday = HolidayCalendar.isMonday(dateYmd),
    itemAvailable = itemAvailable,
    canEquip = canEquip,
    itemImage = itemImage,
)
