package net.sourceforge.kolmafia.request

/** Furniture catalog from desktop ClanRumpusRequest.Equipment. */
object ClanRumpusFurniture {
    enum class Equipment(
        val displayName: String,
        val slot: Int,
        val furni: Int,
        val maxUses: Int,
        val isPvp: Boolean = false,
    ) {
        NONE("", 0, 0, 0),
        GIRL_CALENDAR("Girls of Loathing Calendar", 1, 1, 1),
        BOY_CALENDAR("Boys of Loathing Calendar", 1, 2, 1),
        PAINTING("Infuriating Painting", 1, 3, 1, true),
        MEAT_ORCHID("Exotic Hanging Meat Orchid", 1, 4, 1),
        ARCANE_TOMES("Collection of Arcane Tomes and Whatnot", 2, 1, 0),
        SPORTS_MEMORABILIA("Collection of Sports Memorabilia", 2, 2, 1, true),
        SELF_HELP_BOOKS("Collection of Self-Help Books", 2, 3, 1),
        SODA_MACHINE("Soda Machine", 3, 1, 3),
        JUKEBOX("Jukebox", 3, 2, 0),
        KLAW_GAME("Mr. Klaw \"Skill\" Crane Game", 3, 3, 3),
        RADIO("Old-Timey Radio", 4, 1, 1),
        POTTED_MEAT_BUSH("Potted Meat Bush", 4, 2, 1),
        DESK_CALENDAR("Inspirational Desk Calendar", 4, 3, 1),
        WRESTLING_MAT("Wrestling Mat", 5, 1, 1, true),
        TANNING_BED("Tan-U-Lots Tanning Bed", 5, 2, 0),
        COMFY_SOFA("Comfy Sofa", 5, 3, 0),
        BALLPIT("Awesome Ball Pit", 7, 1, 0),
        HOBO_WORKOUT("Hobo-Flex Workout System", 9, 1, 0),
        SNACK_MACHINE("Snack Machine", 9, 2, 0),
        POTTED_MEAT_TREE("Potted Meat Tree", 9, 3, 1);

        val visitedPreference: String get() = "_clanRumpusSpot${slot}Visited"

        companion object {
            fun equipment(spot: Int, furni: Int): Equipment =
                entries.firstOrNull { it.slot == spot && it.furni == furni } ?: NONE

            fun equipmentName(spot: Int, furni: Int): String =
                if (furni == 0) "" else equipment(spot, furni).displayName
        }
    }
}
