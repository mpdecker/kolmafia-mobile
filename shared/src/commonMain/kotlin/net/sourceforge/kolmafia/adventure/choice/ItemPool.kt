package net.sourceforge.kolmafia.adventure.choice

object ItemPool {
    // Woods items (IDs 1–12): items found via Three-Tined Fork and Footprints
    val WOODS_ITEM_IDS: IntArray = IntArray(12) { it + 1 }

    const val SPOOKY_SAPLING            = 75
    const val TREE_HOLED_COIN           = 4676
    const val INEXPLICABLY_GLOWING_ROCK = 1121
    const val SPOOKY_GLOVE              = 1125
    const val PAPAYA                    = 498
    const val VALUABLE_TRINKET          = 139
    const val MODEL_AIRSHIP             = 6299
    const val MERKIN_PRESSUREGLOBE      = 3675
    const val SEED_PACKET               = 3553
    const val GREEN_SLIME               = 3554
    const val AZAZEL_OBJECT_1           = 2566
    const val AZAZEL_OBJECT_2           = 2567
    const val AZAZEL_OBJECT_3           = 2568
    const val EXPRESS_CARD              = 1687
    const val PICKOMATIC_LOCKPICKS      = 280
    const val SKELETON_KEY              = 642
    const val SILVER_SHOTGUN_SHELL      = 5310
    const val CHAINSAW_CHAIN            = 5309
    const val FUNHOUSE_MIRROR           = 5311
    const val MAXWELL_HAMMER            = 6257
    const val TONGUE_BRACELET           = 6258
    const val SILVER_CHEESE_SLICER      = 6259
    const val SILVER_SHRIMP_FORK        = 6260
    const val SILVER_PATE_KNIFE         = 6261
    const val LOLLIPOP_STICK            = 5380
    const val MCCLUSKY_FILE             = 6689
    const val MCCLUSKY_FILE_PAGE5       = 7039
    const val BINDER_CLIP               = 7040
    const val STONE_TRIANGLE            = 7041
    const val GOLD_1970                 = 8424  // "1,970 carat gold"
    const val NEW_AGE_HEALING_CRYSTAL   = 8701
    const val EMPTY_LAVA_BOTTLE         = 8702
    const val VISCOUS_LAVA_GLOBS        = 8703
    const val GLOWING_NEW_AGE_CRYSTAL   = 8704
    const val CRIMBO_CRYSTAL_SHARDS     = 11066
    const val CRYSTAL_CRIMBO_GOBLET     = 9201
    const val CRYSTAL_CRIMBO_PLATTER    = 9202

    // Chintzy items from Mistress of the Hallowed Halls
    const val CHINTZY_SEAL_PENDANT      = 1941
    const val CHINTZY_TURTLE_BROOCH     = 1942
    const val CHINTZY_NOODLE_RING       = 1943
    const val CHINTZY_SAUCEPAN_EARRING  = 1944
    const val CHINTZY_DISCO_BALL_PENDANT = 1945
    const val CHINTZY_ACCORDION_PIN     = 1946
    const val ANTIQUE_HAND_MIRROR       = 2092

    val MISTRESS_ITEM_IDS: IntArray get() = intArrayOf(
        CHINTZY_SEAL_PENDANT, CHINTZY_TURTLE_BROOCH, CHINTZY_NOODLE_RING,
        CHINTZY_SAUCEPAN_EARRING, CHINTZY_DISCO_BALL_PENDANT, CHINTZY_ACCORDION_PIN,
        ANTIQUE_HAND_MIRROR,
    )
}
