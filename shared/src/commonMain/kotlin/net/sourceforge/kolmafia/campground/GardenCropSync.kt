package net.sourceforge.kolmafia.campground

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [CampgroundRequest.parseGarden] / [CampgroundRequest.findRockGarden] crop yield sync. */
object GardenCropSync {

    private data class CropPattern(
        val gif: String,
        val cropItemId: Int,
        val cropCount: Int,
        val seedItemId: Int = -1,
        val seedCount: Int = 0,
    )

    private val GARDEN_PATTERNS = listOf(
        CropPattern("pumpkinpatch_0.gif", GardenCropIds.PUMPKIN, 0, GardenCropIds.PUMPKIN_SEEDS, 0),
        CropPattern("pumpkinpatch_1.gif", GardenCropIds.PUMPKIN, 1, GardenCropIds.PUMPKIN_SEEDS, 1),
        CropPattern("pumpkinpatch_2.gif", GardenCropIds.PUMPKIN, 2, GardenCropIds.PUMPKIN_SEEDS, 2),
        CropPattern("pumpkinpatch_3.gif", GardenCropIds.PUMPKIN, 3, GardenCropIds.PUMPKIN_SEEDS, 3),
        CropPattern("pumpkinpatch_4.gif", GardenCropIds.PUMPKIN, 4, GardenCropIds.PUMPKIN_SEEDS, 4),
        CropPattern("pumpkinpatch_giant.gif", GardenCropIds.HUGE_PUMPKIN, 1, GardenCropIds.PUMPKIN_SEEDS, 5),
        CropPattern("pumpkinpatch_ginormous.gif", GardenCropIds.GINORMOUS_PUMPKIN, 1, GardenCropIds.PUMPKIN_SEEDS, 11),
        CropPattern("pepperpatch_0.gif", GardenCropIds.PEPPERMINT_SPROUT, 0, GardenCropIds.PEPPERMINT_PACKET, 0),
        CropPattern("pepperpatch_1.gif", GardenCropIds.PEPPERMINT_SPROUT, 3, GardenCropIds.PEPPERMINT_PACKET, 1),
        CropPattern("pepperpatch_2.gif", GardenCropIds.PEPPERMINT_SPROUT, 6, GardenCropIds.PEPPERMINT_PACKET, 2),
        CropPattern("pepperpatch_3.gif", GardenCropIds.PEPPERMINT_SPROUT, 9, GardenCropIds.PEPPERMINT_PACKET, 3),
        CropPattern("pepperpatch_4.gif", GardenCropIds.PEPPERMINT_SPROUT, 12, GardenCropIds.PEPPERMINT_PACKET, 4),
        CropPattern("pepperpatch_huge.gif", GardenCropIds.GIANT_CANDY_CANE, 1, GardenCropIds.PEPPERMINT_PACKET, 5),
        CropPattern("bonegarden0.gif", GardenCropIds.SKELETON, 0, GardenCropIds.DRAGON_TEETH, 0),
        CropPattern("bonegarden1.gif", GardenCropIds.SKELETON, 5, GardenCropIds.DRAGON_TEETH, 1),
        CropPattern("bonegarden2.gif", GardenCropIds.SKELETON, 10, GardenCropIds.DRAGON_TEETH, 2),
        CropPattern("bonegarden3.gif", GardenCropIds.SKELETON, 15, GardenCropIds.DRAGON_TEETH, 3),
        CropPattern("bonegarden4.gif", GardenCropIds.SKELETON, 20, GardenCropIds.DRAGON_TEETH, 4),
        CropPattern("bonegarden5.gif", GardenCropIds.SKELETON, 25, GardenCropIds.DRAGON_TEETH, 5),
        CropPattern("bonegarden_spoilzlul.gif", GardenCropIds.SKELETON, -1, GardenCropIds.DRAGON_TEETH, 6),
        CropPattern("beergarden0.gif", GardenCropIds.BARLEY, 0, GardenCropIds.BEER_SEEDS, 0),
        CropPattern("beergarden1.gif", GardenCropIds.BARLEY, 3, GardenCropIds.BEER_SEEDS, 1),
        CropPattern("beergarden2.gif", GardenCropIds.BARLEY, 6, GardenCropIds.BEER_SEEDS, 2),
        CropPattern("beergarden3.gif", GardenCropIds.FANCY_BEER_LABEL, 1, GardenCropIds.BEER_SEEDS, 3),
        CropPattern("beergarden4.gif", GardenCropIds.FANCY_BEER_LABEL, 1, GardenCropIds.BEER_SEEDS, 4),
        CropPattern("beergarden5.gif", GardenCropIds.FANCY_BEER_LABEL, 2, GardenCropIds.BEER_SEEDS, 5),
        CropPattern("beergarden6.gif", GardenCropIds.FANCY_BEER_LABEL, 2, GardenCropIds.BEER_SEEDS, 6),
        CropPattern("beergarden7.gif", GardenCropIds.FANCY_BEER_LABEL, 3, GardenCropIds.BEER_SEEDS, 7),
        CropPattern("wintergarden0.gif", GardenCropIds.ICE_HARVEST, 0, GardenCropIds.WINTER_SEEDS, 0),
        CropPattern("wintergarden1.gif", GardenCropIds.ICE_HARVEST, 3, GardenCropIds.WINTER_SEEDS, 1),
        CropPattern("wintergarden2.gif", GardenCropIds.ICE_HARVEST, 6, GardenCropIds.WINTER_SEEDS, 2),
        CropPattern("wintergarden3.gif", GardenCropIds.FROST_FLOWER, 1, GardenCropIds.WINTER_SEEDS, 3),
        CropPattern("wintergarden4.gif", GardenCropIds.FROST_FLOWER, 1, GardenCropIds.WINTER_SEEDS, 4),
        CropPattern("wintergarden5.gif", GardenCropIds.FROST_FLOWER, 1, GardenCropIds.WINTER_SEEDS, 5),
        CropPattern("wintergarden6.gif", GardenCropIds.FROST_FLOWER, 1, GardenCropIds.WINTER_SEEDS, 6),
        CropPattern("wintergarden7.gif", GardenCropIds.FROST_FLOWER, 1, GardenCropIds.WINTER_SEEDS, 7),
        CropPattern("thanksgarden1.gif", GardenCropIds.CORNUCOPIA, 0, GardenCropIds.THANKSGARDEN_SEEDS, 0),
        CropPattern("thanksgarden2.gif", GardenCropIds.CORNUCOPIA, 1, GardenCropIds.THANKSGARDEN_SEEDS, 1),
        CropPattern("thanksgarden3.gif", GardenCropIds.CORNUCOPIA, 3, GardenCropIds.THANKSGARDEN_SEEDS, 2),
        CropPattern("thanksgarden4.gif", GardenCropIds.CORNUCOPIA, 5, GardenCropIds.THANKSGARDEN_SEEDS, 3),
        CropPattern("thanksgarden5.gif", GardenCropIds.CORNUCOPIA, 8, GardenCropIds.THANKSGARDEN_SEEDS, 4),
        CropPattern("thanksgarden6.gif", GardenCropIds.CORNUCOPIA, 11, GardenCropIds.THANKSGARDEN_SEEDS, 5),
        CropPattern("thanksgarden7.gif", GardenCropIds.CORNUCOPIA, 15, GardenCropIds.THANKSGARDEN_SEEDS, 6),
        CropPattern("thanksgardenmega.gif", GardenCropIds.MEGACOPIA, 1, GardenCropIds.THANKSGARDEN_SEEDS, 7),
        CropPattern("grassgarden0.gif", GardenCropIds.TALL_GRASS_SEEDS, 0),
        CropPattern("grassgarden1.gif", GardenCropIds.TALL_GRASS_SEEDS, 1),
        CropPattern("grassgarden2.gif", GardenCropIds.TALL_GRASS_SEEDS, 2),
        CropPattern("grassgarden3.gif", GardenCropIds.TALL_GRASS_SEEDS, 3),
        CropPattern("grassgarden4.gif", GardenCropIds.TALL_GRASS_SEEDS, 4),
        CropPattern("grassgarden5.gif", GardenCropIds.TALL_GRASS_SEEDS, 5),
        CropPattern("grassgarden6.gif", GardenCropIds.TALL_GRASS_SEEDS, 6),
        CropPattern("grassgarden7.gif", GardenCropIds.TALL_GRASS_SEEDS, 7),
        CropPattern("grassgarden8.gif", GardenCropIds.TALL_GRASS_SEEDS, 8),
    )

    fun clearCrop(prefs: Preferences?) {
        if (prefs == null) return
        val toClear = GardenCropIds.CROP_ITEM_IDS + GardenCropIds.SEED_ITEM_IDS
        for (itemId in toClear) {
            CampgroundInventorySync.setItem(prefs, itemId, 0)
        }
    }

    fun syncFromHtml(html: String, prefs: Preferences?) {
        if (prefs == null) return
        clearCrop(prefs)

        for (pattern in GARDEN_PATTERNS) {
            if (html.contains(pattern.gif, ignoreCase = true)) {
                applyPattern(prefs, pattern)
                return
            }
        }

        if (html.contains("mushgarden.gif", ignoreCase = true)) {
            val level = prefs.getInt("mushroomGardenCropLevel", 1).coerceAtLeast(1)
            CampgroundInventorySync.setItem(prefs, GardenCropIds.MUSHROOM_SPORES, level)
            return
        }

        syncRockGarden(html, prefs)
    }

    /** Desktop CampgroundRequest.setCampgroundItem(Mushroom) after Mushy Center choice/visit. */
    fun setMushroomCropLevel(prefs: Preferences?, level: Int) {
        if (prefs == null) return
        val capped = level.coerceAtLeast(1)
        clearCrop(prefs)
        CampgroundInventorySync.setItem(prefs, GardenCropIds.MUSHROOM_SPORES, capped)
    }

    private fun applyPattern(prefs: Preferences, pattern: CropPattern) {
        CampgroundInventorySync.setItem(prefs, pattern.cropItemId, pattern.cropCount)
        if (pattern.seedItemId >= 0) {
            CampgroundInventorySync.setItem(prefs, pattern.seedItemId, pattern.seedCount)
        }
    }

    private fun syncRockGarden(html: String, prefs: Preferences) {
        if (!html.contains("/rockgarden/", ignoreCase = true)) return

        var hasSomething = false

        if (containsRock(prefs, html, "rockgarden/a1.gif", GardenCropIds.GROVELING_GRAVEL, 1) ||
            containsRock(prefs, html, "rockgarden/a2.gif", GardenCropIds.GROVELING_GRAVEL, 2) ||
            containsRock(prefs, html, "rockgarden/a3.gif", GardenCropIds.GROVELING_GRAVEL, 3) ||
            containsRock(prefs, html, "rockgarden/a4.gif", GardenCropIds.FRUITY_PEBBLE, 1) ||
            containsRock(prefs, html, "rockgarden/a5.gif", GardenCropIds.FRUITY_PEBBLE, 2) ||
            containsRock(prefs, html, "rockgarden/a6.gif", GardenCropIds.FRUITY_PEBBLE, 3) ||
            containsRock(prefs, html, "rockgarden/a7.gif", GardenCropIds.LODESTONE, 1)
        ) {
            hasSomething = true
        } else {
            containsRock(prefs, html, "rockgarden/a0.gif", GardenCropIds.GROVELING_GRAVEL, 0)
        }

        if (containsRock(prefs, html, "rockgarden/b1.gif", GardenCropIds.MILESTONE, 1) ||
            containsRock(prefs, html, "rockgarden/b2.gif", GardenCropIds.MILESTONE, 2) ||
            containsRock(prefs, html, "rockgarden/b3.gif", GardenCropIds.MILESTONE, 3) ||
            containsRock(prefs, html, "rockgarden/b4.gif", GardenCropIds.BOLDER_BOULDER, 1) ||
            containsRock(prefs, html, "rockgarden/b5.gif", GardenCropIds.BOLDER_BOULDER, 2) ||
            containsRock(prefs, html, "rockgarden/b6.gif", GardenCropIds.BOLDER_BOULDER, 3) ||
            containsRock(prefs, html, "rockgarden/b7.gif", GardenCropIds.MOLEHILL_MOUNTAIN, 1)
        ) {
            hasSomething = true
        } else {
            containsRock(prefs, html, "rockgarden/b0.gif", GardenCropIds.MILESTONE, 0)
        }

        if (containsRock(prefs, html, "rockgarden/c1.gif", GardenCropIds.WHETSTONE, 1) ||
            containsRock(prefs, html, "rockgarden/c2.gif", GardenCropIds.WHETSTONE, 2) ||
            containsRock(prefs, html, "rockgarden/c3.gif", GardenCropIds.WHETSTONE, 3) ||
            containsRock(prefs, html, "rockgarden/c4.gif", GardenCropIds.HARD_ROCK, 1) ||
            containsRock(prefs, html, "rockgarden/c5.gif", GardenCropIds.HARD_ROCK, 2) ||
            containsRock(prefs, html, "rockgarden/c6.gif", GardenCropIds.HARD_ROCK, 3) ||
            containsRock(prefs, html, "rockgarden/c7.gif", GardenCropIds.STRANGE_STALAGMITE, 1)
        ) {
            hasSomething = true
        } else {
            containsRock(prefs, html, "rockgarden/c0.gif", GardenCropIds.WHETSTONE, 0)
        }

        CampgroundInventorySync.setItem(
            prefs,
            GardenCropIds.ROCK_SEEDS,
            if (hasSomething) 1 else 0,
        )
    }

    private fun containsRock(prefs: Preferences, html: String, gif: String, itemId: Int, count: Int): Boolean {
        if (!html.contains(gif, ignoreCase = true)) return false
        CampgroundInventorySync.setItem(prefs, itemId, count)
        return true
    }
}
