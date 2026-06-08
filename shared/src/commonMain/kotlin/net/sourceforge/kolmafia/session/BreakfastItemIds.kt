package net.sourceforge.kolmafia.session

/**
 * Compile-time constants for all item IDs used by BreakfastManager.
 *
 * Item IDs verified against desktop KoLmafia ItemPool.java and
 * BreakfastManager.java (toy array, lines 57-93).
 *
 * TOYS map: item ID → number of times to use per day.
 */
object BreakfastItemIds {

    // ── Hermit trade ─────────────────────────────────────────────────────────
    const val CLOVER_ITEM_ID              = 24     // 11-leaf clover (item to RECEIVE)
    const val WORTHLESS_TRINKET_ID        = 7
    const val WORTHLESS_KNICK_KNACK_ID    = 8
    const val WORTHLESS_GEWGAW_ID         = 9

    // ── Genie bottle / pocket wish ────────────────────────────────────────────
    const val GENIE_BOTTLE_ID             = 9529
    const val REPLICA_GENIE_BOTTLE_ID     = 11234

    // ── One-time item uses ────────────────────────────────────────────────────
    const val BOOK_OF_EVERY_SKILL_ID      = 10917
    const val REPLICA_SNOWCONE_ID         = 11197
    const val REPLICA_RESOLUTION_ID       = 11213
    const val REPLICA_SMITH_ID            = 11219
    const val ALLIED_RADIO_BACKPACK_ID    = 11933
    const val ANTICHEESE_ID               = 142
    const val APRIL_SHOWER_THOUGHTS_SHIELD = 11884
    const val MR_STORE_2002_CATALOG_ID    = 11257
    const val REPLICA_MR_STORE_CATALOG_ID = 11280

    // ── 34-toy map: item ID → daily use count ─────────────────────────────────
    val TOYS: Map<Int, Int> = mapOf(
        3092 to 1,   // hobby horse
        3093 to 1,   // ball-in-a-cup
        3094 to 1,   // set of jacks
        3261 to 1,   // bag of candy
        3010 to 1,   // Emblem of Akgyxoth
        3009 to 1,   // Idol of Akgyxoth
        3629 to 1,   // Burrowgrub hive
        3731 to 1,   // gnoll eye
        4641 to 1,   // KoL Con six-pack
        5502 to 1,   // Trivial Avocations game
        5062 to 1,   // creepy voodoo doll
        5664 to 1,   // cursed keg
        5663 to 1,   // cursed microwave
        6051 to 1,   // taco flier
        7059 to 1,   // WarBear soda machine
        7056 to 1,   // WarBear breakfast machine
        7060 to 1,   // WarBear bank
        7729 to 1,   // Chroner trigger
        7723 to 1,   // Chroner cross
        7936 to 1,   // picky tweezers
        8283 to 1,   // cocktail shaker
        9025 to 1,   // bacon machine
        637  to 1,   // toaster
        9123 to 11,  // school of hard knocks diploma (11 uses/day)
        5739 to 1,   // CSA fire-starting kit
        9961 to 3,   // pump-up high tops (3 uses/day)
        10265 to 1,  // etched hourglass
        10207 to 1,  // glitch item
        10652 to 1,  // subscription cocoa dispenser
        10670 to 1,  // overflowing gift basket
        10878 to 1,  // meatball machine
        10879 to 1,  // refurbished air fryer
        11451 to 1,  // punching mirror
        11485 to 1,  // li'l snowball factory
    )
}
