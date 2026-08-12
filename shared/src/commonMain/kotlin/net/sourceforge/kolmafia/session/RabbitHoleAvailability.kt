package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [RabbitHoleManager] hat-length data for maximizer hatter buff availability. */
object RabbitHoleAvailability {

    const val DRINK_ME_POTION_ID = 4508
    const val DOWN_THE_RABBIT_HOLE_EFFECT = 725

    data class Hat(
        val length: Int,
        val effect: String,
        val modifier: String,
    )

    val HAT_DATA = arrayOf(
        Hat(4, "Assaulted with Pepper", "Monster Level +20"),
        Hat(6, "Three Days Slow", "Familiar Experience +3"),
        Hat(7, "Cat-Alyzed", "Moxie +10"),
        Hat(8, "Anytwo Five Elevenis?", "Muscle +10"),
        Hat(9, "Coated Arms", "Weapon Damage +15"),
        Hat(10, "Smoky Third Eye", "Mysticality +10"),
        Hat(11, "Full Bottle in front of Me", "Spell Damage +30%"),
        Hat(12, "Thick-Skinned", "Maximum HP +50"),
        Hat(13, "20-20 Second Sight", "Maximum MP +25"),
        Hat(14, "Slimy Hands", "+10 Sleaze Damage"),
        Hat(15, "Bottle in front of Me", "Spell Damage +15"),
        Hat(16, "Fan-Cooled", "+10 Cold Damage"),
        Hat(17, "Ginger Snapped", "+10 Spooky Damage"),
        Hat(18, "Egg on your Face", "+10 Stench Damage"),
        Hat(19, "Pockets of Fire", "+10 Hot Damage"),
        Hat(20, "Weapon of Mass Destruction", "Weapon Damage +30%"),
        Hat(21, "Orchid Blood", "Regenerate 5-10 MP per Adventure"),
        Hat(22, "Dances with Tweedles", "+40% Meat from Monsters"),
        Hat(23, "Patched In", "Mysticality +20%"),
        Hat(24, "You Can Really Taste the Dormouse", "+5 to Familiar Weight"),
        Hat(25, "Turtle Titters", "+3 Stat Gains from Fights"),
        Hat(26, "Cat Class, Cat Style", "Moxie +20%"),
        Hat(27, "Surreally Buff", "Muscle +20%"),
        Hat(28, "Quadrilled", "+20% Items from Monsters"),
        Hat(29, "Coming Up Roses", "Regenerate 10-20 MP per Adventure"),
        Hat(30, "Oleaginous Soles", "+40% Combat Initiative"),
        Hat(31, "Oleaginous Soles", "+40% Combat Initiative"),
    )

    private val hatCleaner = Regex("[^a-zA-Z]")

    fun hatLength(name: String): Int = hatCleaner.replace(name, "").length

    fun teaPartyAvailable(prefs: Preferences?): Boolean =
        prefs?.getBoolean("_madTeaParty", false) != true

    fun hatDataForLength(desiredHatLength: Int): Hat? =
        HAT_DATA.firstOrNull { it.length == desiredHatLength }

    /** First inventory/equipped hat whose letter-length matches [desiredHatLength]. */
    fun findHatNameForLength(
        desiredHatLength: Int,
        inventoryCount: (Int) -> Int,
        equippedHatName: String?,
    ): String? {
        if (!equippedHatName.isNullOrBlank() && hatLength(equippedHatName) == desiredHatLength) {
            return equippedHatName
        }
        for (item in ItemDatabase.all()) {
            if (item.primaryUse != ItemPrimaryUse.HAT) continue
            if (inventoryCount(item.id) <= 0) continue
            if (hatLength(item.name) == desiredHatLength) return item.name
        }
        return null
    }

    fun findHatIdForLength(
        desiredHatLength: Int,
        inventoryCount: (Int) -> Int,
        equippedHatName: String?,
    ): Int? {
        val name = findHatNameForLength(desiredHatLength, inventoryCount, equippedHatName)
            ?: return null
        if (!equippedHatName.isNullOrBlank() &&
            equippedHatName.equals(name, ignoreCase = true) &&
            hatLength(equippedHatName) == desiredHatLength
        ) {
            // Prefer equipped hat's id when name matches equipped
            ItemDatabase.getByName(equippedHatName)?.id?.let { return it }
        }
        return ItemDatabase.getByName(name)?.id
            ?: ItemDatabase.all().firstOrNull {
                it.primaryUse == ItemPrimaryUse.HAT &&
                    inventoryCount(it.id) > 0 &&
                    hatLength(it.name) == desiredHatLength
            }?.id
    }

    fun hatLengthAvailable(
        desiredHatLength: Int,
        inventoryCount: (Int) -> Int,
        equippedHatName: String?,
    ): Boolean {
        val lengths = mutableSetOf<Int>()
        if (!equippedHatName.isNullOrBlank()) {
            lengths += hatLength(equippedHatName)
        }
        for (item in ItemDatabase.all()) {
            if (item.primaryUse != ItemPrimaryUse.HAT) continue
            if (inventoryCount(item.id) <= 0) continue
            lengths += hatLength(item.name)
        }
        return desiredHatLength in lengths
    }
}
