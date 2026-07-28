package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Parses equipment.txt from bundled compose resources.
// The file is divided into sections (Hats, Weapons, etc.).
// Each data line is tab-separated: name  power  stat_requirement  [hands/type]
@OptIn(ExperimentalResourceApi::class)
object EquipmentDatabase {

    private val byName = mutableMapOf<String, EquipmentData>()
    private val byItemId = mutableMapOf<Int, EquipmentData>()
    private var loaded = false

    private val handsTypePattern = Regex("""(\d+)-handed\s+(.+)""")

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/equipment.txt").decodeToString()
        parse(text)
        rebuildByItemId()
        loaded = true
    }

    fun getByName(name: String): EquipmentData? = byName[name.lowercase()]
    fun getByItemId(itemId: Int): EquipmentData? = byItemId[itemId]
    fun all(): Collection<EquipmentData> = byName.values

    fun getPower(itemId: Int): Int = byItemId[itemId]?.power ?: 0

    fun getHands(itemId: Int): Int = byItemId[itemId]?.hands ?: 0

    fun getItemType(itemId: Int): String {
        val item = ItemDatabase.getById(itemId) ?: return ""
        byItemId[itemId]?.itemType?.let { return it }
        return primaryUseItemType(item.primaryUse, itemId)
    }

    fun getWeaponStat(itemId: Int): WeaponStat {
        val item = ItemDatabase.getById(itemId) ?: return WeaponStat.NONE
        if (item.primaryUse != ItemPrimaryUse.WEAPON) return WeaponStat.NONE
        val req = byItemId[itemId]?.statRequirement.orEmpty()
        return when {
            req.startsWith("Mox:", ignoreCase = true) -> WeaponStat.MOXIE
            req.startsWith("Mys:", ignoreCase = true) -> WeaponStat.MYSTICALITY
            else -> WeaponStat.MUSCLE
        }
    }

    /** Test hook — inject equipment without loading equipment.txt. */
    internal fun registerForTest(itemId: Int, equipment: EquipmentData) {
        byName[equipment.name.lowercase()] = equipment
        byItemId[itemId] = equipment
        loaded = true
    }

    internal fun resetForTest() {
        byName.clear()
        byItemId.clear()
        loaded = false
    }

    private fun rebuildByItemId() {
        byItemId.clear()
        for (equipment in byName.values) {
            val itemId = ItemDatabase.getByName(equipment.name)?.id ?: continue
            byItemId[itemId] = equipment
        }
    }

    private fun primaryUseItemType(primaryUse: ItemPrimaryUse, itemId: Int): String = when (primaryUse) {
        ItemPrimaryUse.FOOD -> "food"
        ItemPrimaryUse.DRINK -> "booze"
        ItemPrimaryUse.SPLEEN -> "spleen item"
        ItemPrimaryUse.FOOD_HELPER -> "food helper"
        ItemPrimaryUse.DRINK_HELPER -> "drink helper"
        ItemPrimaryUse.STICKER -> "sticker"
        ItemPrimaryUse.CARD -> "card"
        ItemPrimaryUse.FOLDER -> "folder"
        ItemPrimaryUse.BOOTSKIN -> "bootskin"
        ItemPrimaryUse.BOOTSPUR -> "bootspur"
        ItemPrimaryUse.SIXGUN -> "sixgun"
        ItemPrimaryUse.POTION -> "potion"
        ItemPrimaryUse.AVATAR -> "avatar potion"
        ItemPrimaryUse.FAMILIAR -> "familiar larva"
        ItemPrimaryUse.ZAP -> "zap wand"
        ItemPrimaryUse.ACCESSORY -> "accessory"
        ItemPrimaryUse.HAT -> "hat"
        ItemPrimaryUse.PANTS -> "pants"
        ItemPrimaryUse.SHIRT -> "shirt"
        ItemPrimaryUse.WEAPON -> byItemId[itemId]?.itemType ?: "weapon"
        ItemPrimaryUse.OFFHAND -> byItemId[itemId]?.itemType ?: "offhand"
        ItemPrimaryUse.CONTAINER -> "container"
        ItemPrimaryUse.GUARDIAN -> "pasta guardian"
        else -> ""
    }

    private fun parse(text: String) {
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t')) continue

            val parts = line.split('\t')
            if (parts.size < 2) continue

            val name = parts[0].trim()
            val power = parts[1].trim().toIntOrNull() ?: continue
            val statReq = parts.getOrNull(2)?.trim()?.let {
                if (it == "none" || it.isBlank()) null else it
            }
            var hands = 0
            var itemType: String? = null
            parts.getOrNull(3)?.trim()?.takeIf { it.isNotBlank() }?.let { extra ->
                val match = handsTypePattern.matchEntire(extra)
                if (match != null) {
                    hands = match.groupValues[1].toIntOrNull() ?: 0
                    itemType = match.groupValues[2].trim()
                } else {
                    itemType = extra
                }
            }

            val equip = EquipmentData(name, power, statReq, hands, itemType)
            byName[name.lowercase()] = equip
        }
    }
}
