package net.sourceforge.kolmafia.data

/** Item IDs excluded from TCRS modifier application (desktop TCRSDatabase.applyModifiers skips). */
object TCRSSkipItemIds {

/**
 * Campground housing/bedding/workshed items with bundled modifiers.txt entries.
 * Source: desktop CampgroundRequest / ChateauRequest campgroundItems/chateauItems.
 */
val CAMPGROUND_ITEMS: Set<Int> = setOf(
    30, 69, 73, 101, 104, 143, 157, 210,
    236, 429, 438, 440, 502, 526, 636, 1000,
    1111, 1112, 1113, 1311, 1923, 2638, 3127, 3198,
    3276, 3277, 3281, 3344, 3345, 3346, 3347, 3348,
    3374, 3416, 4347, 4485, 4707, 4708, 4771, 4842,
    5888, 6072, 6120, 6122, 6338, 6614, 6668, 6773,
    6890, 6964, 6965, 6966, 6967, 7036, 7037, 7082,
    7089, 7140, 7295, 7350, 7382, 7758, 8260, 8639,
    8989, 9033, 9185, 9508, 10072, 10335, 10497, 10815,
    11045, 11262, 11268, 11340, 11345, 11377, 11687, 11891,
)

/**
 * Chateau furniture items with bundled modifiers.txt entries.
 * Source: desktop CampgroundRequest / ChateauRequest campgroundItems/chateauItems.
 */
val CHATEAU_ITEMS: Set<Int> = setOf(
    8023, 8024, 8025, 8026, 8027, 8028, 8029, 8030,
    8031, 8033,
)
}
