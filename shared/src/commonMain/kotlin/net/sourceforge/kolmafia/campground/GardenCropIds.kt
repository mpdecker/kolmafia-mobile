package net.sourceforge.kolmafia.campground

/** Desktop [CampgroundRequest.CROPS] / [CampgroundRequest.CropType] item-id constants. */
internal object GardenCropIds {
    const val PUMPKIN = 4761
    const val HUGE_PUMPKIN = 4762
    const val GINORMOUS_PUMPKIN = 4771
    const val PUMPKIN_SEEDS = 4760

    const val PEPPERMINT_SPROUT = 5395
    const val GIANT_CANDY_CANE = 5402
    const val PEPPERMINT_PACKET = 5404

    const val SKELETON = 5881
    const val DRAGON_TEETH = 5880

    const val BARLEY = 6752
    const val FANCY_BEER_LABEL = 6755
    const val BEER_SEEDS = 6751

    const val ICE_HARVEST = 7072
    const val FROST_FLOWER = 7073
    const val WINTER_SEEDS = 7070

    const val CORNUCOPIA = 9183
    const val MEGACOPIA = 9184
    const val THANKSGARDEN_SEEDS = 9186

    const val TALL_GRASS_SEEDS = 9760

    const val MUSHROOM_SPORES = 10482

    const val ROCK_SEEDS = 11100
    const val GROVELING_GRAVEL = 11101
    const val FRUITY_PEBBLE = 11102
    const val LODESTONE = 11103
    const val MILESTONE = 11104
    const val BOLDER_BOULDER = 11105
    const val MOLEHILL_MOUNTAIN = 11106
    const val WHETSTONE = 11107
    const val HARD_ROCK = 11108
    const val STRANGE_STALAGMITE = 11109

    val CROP_ITEM_IDS: Set<Int> = setOf(
        PUMPKIN, HUGE_PUMPKIN, GINORMOUS_PUMPKIN,
        PEPPERMINT_SPROUT, GIANT_CANDY_CANE,
        SKELETON,
        BARLEY, FANCY_BEER_LABEL,
        ICE_HARVEST, FROST_FLOWER,
        CORNUCOPIA, MEGACOPIA,
        TALL_GRASS_SEEDS,
        MUSHROOM_SPORES,
        GROVELING_GRAVEL, FRUITY_PEBBLE, LODESTONE, MILESTONE,
        BOLDER_BOULDER, MOLEHILL_MOUNTAIN, WHETSTONE, HARD_ROCK, STRANGE_STALAGMITE,
    )

    val SEED_ITEM_IDS: Set<Int> = setOf(
        PUMPKIN_SEEDS,
        PEPPERMINT_PACKET,
        DRAGON_TEETH,
        BEER_SEEDS,
        WINTER_SEEDS,
        THANKSGARDEN_SEEDS,
        TALL_GRASS_SEEDS,
        MUSHROOM_SPORES,
        ROCK_SEEDS,
    )

    val CROP_NAME_TO_ID: Map<String, Int> = mapOf(
        "pumpkin" to PUMPKIN,
        "huge pumpkin" to HUGE_PUMPKIN,
        "ginormous pumpkin" to GINORMOUS_PUMPKIN,
        "peppermint sprout" to PEPPERMINT_SPROUT,
        "giant candy cane" to GIANT_CANDY_CANE,
        "skeleton bone" to SKELETON,
        "skeleton" to SKELETON,
        "barley" to BARLEY,
        "fancy beer label" to FANCY_BEER_LABEL,
        "ice harvest" to ICE_HARVEST,
        "frost flower" to FROST_FLOWER,
        "cornucopia" to CORNUCOPIA,
        "megacopia" to MEGACOPIA,
    )

    /** Desktop [CampgroundRequest.CROPS] ordering for [GardenCropAvailability.hasCropOrBetter]. */
    val CROPS_ORDER: List<Int> = listOf(
        PUMPKIN, HUGE_PUMPKIN, GINORMOUS_PUMPKIN,
        PEPPERMINT_SPROUT, GIANT_CANDY_CANE,
        SKELETON,
        BARLEY, FANCY_BEER_LABEL, FANCY_BEER_LABEL, FANCY_BEER_LABEL,
        ICE_HARVEST, FROST_FLOWER,
        CORNUCOPIA, CORNUCOPIA, CORNUCOPIA, CORNUCOPIA, CORNUCOPIA, CORNUCOPIA,
        MEGACOPIA,
        TALL_GRASS_SEEDS, TALL_GRASS_SEEDS,
        MUSHROOM_SPORES, MUSHROOM_SPORES, MUSHROOM_SPORES, MUSHROOM_SPORES,
        MUSHROOM_SPORES, MUSHROOM_SPORES,
        GROVELING_GRAVEL, FRUITY_PEBBLE, LODESTONE, MILESTONE,
        BOLDER_BOULDER, MOLEHILL_MOUNTAIN, WHETSTONE, HARD_ROCK, STRANGE_STALAGMITE,
    )

    /** Desktop [CampgroundRequest.CROPS] scan order for [GardenCropAvailability.getCrop]. */
    val CROPS_SCAN_ORDER: List<Int> = listOf(
        PUMPKIN, HUGE_PUMPKIN, GINORMOUS_PUMPKIN,
        PEPPERMINT_SPROUT, GIANT_CANDY_CANE,
        SKELETON,
        BARLEY, FANCY_BEER_LABEL,
        ICE_HARVEST, FROST_FLOWER,
        CORNUCOPIA, MEGACOPIA,
        TALL_GRASS_SEEDS,
        MUSHROOM_SPORES,
        GROVELING_GRAVEL, FRUITY_PEBBLE, LODESTONE, MILESTONE,
        BOLDER_BOULDER, MOLEHILL_MOUNTAIN, WHETSTONE, HARD_ROCK, STRANGE_STALAGMITE,
    )

    val CROP_TYPE_BY_ITEM: Map<Int, CropType> = mapOf(
        PUMPKIN to CropType.PUMPKIN,
        HUGE_PUMPKIN to CropType.PUMPKIN,
        GINORMOUS_PUMPKIN to CropType.PUMPKIN,
        PEPPERMINT_SPROUT to CropType.PEPPERMINT,
        GIANT_CANDY_CANE to CropType.PEPPERMINT,
        SKELETON to CropType.SKELETON,
        BARLEY to CropType.BEER,
        FANCY_BEER_LABEL to CropType.BEER,
        ICE_HARVEST to CropType.WINTER,
        FROST_FLOWER to CropType.WINTER,
        CORNUCOPIA to CropType.THANKSGARDEN,
        MEGACOPIA to CropType.THANKSGARDEN,
        TALL_GRASS_SEEDS to CropType.GRASS,
        MUSHROOM_SPORES to CropType.MUSHROOM,
        GROVELING_GRAVEL to CropType.ROCK,
        FRUITY_PEBBLE to CropType.ROCK,
        LODESTONE to CropType.ROCK,
        MILESTONE to CropType.ROCK,
        BOLDER_BOULDER to CropType.ROCK,
        MOLEHILL_MOUNTAIN to CropType.ROCK,
        WHETSTONE to CropType.ROCK,
        HARD_ROCK to CropType.ROCK,
        STRANGE_STALAGMITE to CropType.ROCK,
    )
}
