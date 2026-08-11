package net.sourceforge.kolmafia.campground

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [CampgroundRequest.getCrop] / [CampgroundRequest.hasCropOrBetter] breakfast helpers. */
object GardenCropAvailability {

    data class Crop(val itemId: Int, val count: Int)

    fun getCrop(prefs: Preferences?): Crop? {
        if (prefs == null) return null
        val cache = CampgroundInventorySync.load(prefs)
        for (itemId in GardenCropIds.CROPS_SCAN_ORDER) {
            val count = cache[itemId] ?: continue
            if (count != 0) return Crop(itemId, count)
        }
        return null
    }

    fun parseCrop(name: String): Crop {
        var cropName = name.trim()
        var count = 1

        val paren = cropName.indexOf(" (")
        if (paren != -1) {
            count = cropName.substring(paren + 2, cropName.length - 1).trim().toIntOrNull() ?: 1
            cropName = cropName.substring(0, paren).trim()
        }

        return when (cropName.lowercase()) {
            "tall grass" -> Crop(GardenCropIds.TALL_GRASS_SEEDS, count)
            "very tall grass" -> Crop(GardenCropIds.TALL_GRASS_SEEDS, 8)
            "free-range mushroom" -> Crop(GardenCropIds.MUSHROOM_SPORES, 1)
            "plump free-range mushroom" -> Crop(GardenCropIds.MUSHROOM_SPORES, 2)
            "bulky free-range mushroom" -> Crop(GardenCropIds.MUSHROOM_SPORES, 3)
            "giant free-range mushroom" -> Crop(GardenCropIds.MUSHROOM_SPORES, 4)
            "immense free-range mushroom" -> Crop(GardenCropIds.MUSHROOM_SPORES, 5)
            "colossal free-range mushroom" -> Crop(GardenCropIds.MUSHROOM_SPORES, 11)
            else -> {
                val itemId = GardenCropIds.CROP_NAME_TO_ID[cropName.lowercase()] ?: -1
                Crop(itemId, count)
            }
        }
    }

    fun hasCropOrBetter(prefs: Preferences?, cropName: String): Boolean =
        hasCropOrBetter(getCrop(prefs), cropName)

    fun hasCropOrBetter(current: Crop?, cropName: String): Boolean {
        if (current == null || current.count == 0 || cropName.equals("none", ignoreCase = true)) {
            return false
        }
        if (cropName.equals("any", ignoreCase = true)) {
            return true
        }

        val desired = parseCrop(cropName)
        if (desired.itemId < 0) return false

        if (current.itemId == desired.itemId) {
            return current.count >= desired.count
        }

        for (cropItemId in GardenCropIds.CROPS_ORDER) {
            if (cropItemId == current.itemId) {
                return false
            }
            if (cropItemId == desired.itemId) {
                val currentType = GardenCropIds.CROP_TYPE_BY_ITEM[current.itemId]
                val desiredType = GardenCropIds.CROP_TYPE_BY_ITEM[desired.itemId]
                return currentType != null && currentType == desiredType
            }
        }

        return false
    }
}
