package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Parses equipment.txt from bundled compose resources.
// The file is divided into sections (Hats, Weapons, etc.).
// Each data line is tab-separated: name  power  stat_requirement  [hands/type]
@OptIn(ExperimentalResourceApi::class)
object EquipmentDatabase {

    const val USELESS_POWDER = 1437

    private val PULVERIZABLE_USES = setOf(
        ItemPrimaryUse.ACCESSORY,
        ItemPrimaryUse.HAT,
        ItemPrimaryUse.PANTS,
        ItemPrimaryUse.SHIRT,
        ItemPrimaryUse.WEAPON,
        ItemPrimaryUse.OFFHAND,
        ItemPrimaryUse.CONTAINER,
    )

    private val byName = mutableMapOf<String, EquipmentData>()
    private val byItemId = mutableMapOf<Int, EquipmentData>()
    private val pulverizeByItemId = mutableMapOf<Int, Int>()
    private var loaded = false

    private val handsTypePattern = Regex("""(\d+)-handed\s+(.+)""")

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/equipment.txt").decodeToString()
        parse(text)
        rebuildByItemId()
        val pulverizeText = Res.readBytes("files/data/pulverize.txt").decodeToString()
        loadPulverizeFromText(pulverizeText)
        loaded = true
    }

    fun getByName(name: String): EquipmentData? = byName[name.lowercase()]
    fun getByItemId(itemId: Int): EquipmentData? = byItemId[itemId]
    fun all(): Collection<EquipmentData> = byName.values

    fun contains(itemId: Int): Boolean = byItemId.containsKey(itemId)

    /** Desktop [EquipmentDatabase.nextEquipmentItemId] — next equipment/familiar/sixgun item id. */
    fun nextEquipmentItemId(prevId: Int): Int {
        var id = prevId
        val limit = ItemDatabase.maxItemId()
        while (++id <= limit) {
            if (byItemId.containsKey(id)) return id
            val item = ItemDatabase.getById(id) ?: continue
            if (item.primaryUse == ItemPrimaryUse.FAMILIAR || item.primaryUse == ItemPrimaryUse.SIXGUN) {
                return id
            }
        }
        return -1
    }

    fun allEquipmentItemIds(): Sequence<Int> = sequence {
        var id = 0
        while (true) {
            id = nextEquipmentItemId(id)
            if (id == -1) break
            yield(id)
        }
    }

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

    /** Desktop [EquipmentDatabase.getWeaponType] — melee vs ranged. */
    fun getWeaponType(itemId: Int): WeaponType = when (getWeaponStat(itemId)) {
        WeaponStat.NONE -> WeaponType.NONE
        WeaponStat.MOXIE -> WeaponType.RANGED
        else -> WeaponType.MELEE
    }

    fun isChefStaff(itemId: Int): Boolean =
        getItemType(itemId).equals("chefstaff", ignoreCase = true)

    fun isClub(itemId: Int, ironPalms: Boolean = false): Boolean {
        val type = getItemType(itemId).lowercase()
        return type == "club" || (ironPalms && type == "sword")
    }

    fun isSword(itemId: Int): Boolean =
        getItemType(itemId).equals("sword", ignoreCase = true)

    fun isKnife(itemId: Int): Boolean =
        getItemType(itemId).equals("knife", ignoreCase = true)

    fun isUtensil(itemId: Int): Boolean =
        getItemType(itemId).equals("utensil", ignoreCase = true)

    fun isAccordion(itemId: Int): Boolean =
        getItemType(itemId).contains("accordion", ignoreCase = true)

    fun isShield(itemId: Int): Boolean =
        getItemType(itemId).equals("shield", ignoreCase = true)

    fun isGun(itemId: Int): Boolean =
        getItemType(itemId).equals("gun", ignoreCase = true)

    fun isPistol(itemId: Int): Boolean =
        getItemType(itemId).equals("pistol", ignoreCase = true)

    fun isRifle(itemId: Int): Boolean =
        getItemType(itemId).equals("rifle", ignoreCase = true)

    fun addPulverization(itemId: Int, result: Int) {
        pulverizeByItemId[itemId] = result
    }

    fun isPulverizable(itemId: Int): Boolean {
        if (itemId < 0) return false
        val item = ItemDatabase.getById(itemId) ?: return false
        if (item.primaryUse !in PULVERIZABLE_USES) return false
        if (ItemDatabase.isQuestItem(itemId)) return false
        return true
    }

    fun getPulverization(itemId: Int): Int {
        if (itemId < 0) return -1
        pulverizeByItemId[itemId]?.let { return it }
        val derived = derivePulverization(itemId)
        pulverizeByItemId[itemId] = derived
        return derived
    }

    fun initializePulverization() {
        for (item in ItemDatabase.all()) {
            if (item.descId.isBlank()) continue
            if (isPulverizable(item.id)) {
                getPulverization(item.id)
            }
        }
    }

    internal fun derivePulverization(itemId: Int): Int {
        if (!isPulverizable(itemId)) return -1

        val item = ItemDatabase.getById(itemId) ?: return -1
        if ('g' in item.access && !item.isTradeable) {
            return USELESS_POWDER
        }

        if (NpcStoreDatabase.containsItem(itemId, validate = false)) {
            return USELESS_POWDER
        }

        var pulver = PulverizeFlags.PULVERIZE_BITS or PulverizeFlags.ELEM_TWINKLY
        val entry = ModifierDatabase.getItem(item.name)
        if (entry != null) {
            pulver = PulverizeImplications.apply(ModifierParser.parse(entry.modifiers), pulver)
        }

        val power = getPower(itemId)
        pulver = pulver or when {
            power >= 180 -> PulverizeFlags.YIELD_3W
            power >= 160 -> PulverizeFlags.YIELD_1W3N_2W
            power >= 140 -> PulverizeFlags.YIELD_4N_1W
            power >= 120 -> PulverizeFlags.YIELD_3N
            power >= 100 -> PulverizeFlags.YIELD_1N3P_2N
            power >= 80 -> PulverizeFlags.YIELD_4P_1N
            power >= 60 -> PulverizeFlags.YIELD_3P
            power >= 40 -> PulverizeFlags.YIELD_2P
            else -> PulverizeFlags.YIELD_1P
        }
        return pulver
    }

    internal fun loadPulverizeFromText(text: String) {
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val parts = line.split('\t')
            if (parts.size < 2) continue

            val itemName = parts[0].trim()
            val itemId = ItemDatabase.getByName(itemName)?.id ?: continue
            val spec = parts[1].trim()

            val result = when {
                spec.equals("nosmash", ignoreCase = true) -> -1
                spec.equals("upgrade", ignoreCase = true) -> deriveUpgrade(itemName)
                spec.toIntOrNull() != null ->
                    PulverizeFlags.PULVERIZE_BITS or spec.toInt()
                spec.endsWith("cluster", ignoreCase = true) -> deriveCluster(spec)
                else -> ItemDatabase.getByName(spec)?.id ?: continue
            }
            addPulverization(itemId, result)
        }
    }

    internal fun deriveUpgrade(name: String): Int {
        var pulver = PulverizeFlags.PULVERIZE_BITS or
            PulverizeFlags.MALUS_UPGRADE or
            PulverizeFlags.YIELD_4N_1W
        if (name.endsWith("powder", ignoreCase = true)) {
            pulver = pulver or PulverizeFlags.YIELD_4P_1N
        }

        pulver = pulver or when {
            name.startsWith("twinkly", ignoreCase = true) -> PulverizeFlags.ELEM_TWINKLY
            name.startsWith("hot", ignoreCase = true) -> PulverizeFlags.ELEM_HOT
            name.startsWith("cold", ignoreCase = true) -> PulverizeFlags.ELEM_COLD
            name.startsWith("stench", ignoreCase = true) -> PulverizeFlags.ELEM_STENCH
            name.startsWith("spook", ignoreCase = true) -> PulverizeFlags.ELEM_SPOOKY
            name.startsWith("sleaz", ignoreCase = true) -> PulverizeFlags.ELEM_SLEAZE
            else -> PulverizeFlags.ELEM_OTHER
        }
        return pulver
    }

    internal fun deriveCluster(spec: String): Int {
        var pulver = PulverizeFlags.PULVERIZE_BITS or PulverizeFlags.YIELD_1C
        pulver = when {
            spec.startsWith("hot", ignoreCase = true) -> pulver or PulverizeFlags.ELEM_HOT
            spec.startsWith("cold", ignoreCase = true) -> pulver or PulverizeFlags.ELEM_COLD
            spec.startsWith("stench", ignoreCase = true) -> pulver or PulverizeFlags.ELEM_STENCH
            spec.startsWith("spook", ignoreCase = true) -> pulver or PulverizeFlags.ELEM_SPOOKY
            spec.startsWith("sleaz", ignoreCase = true) -> pulver or PulverizeFlags.ELEM_SLEAZE
            else -> ItemDatabase.getByName(spec)?.id ?: -1
        }
        return pulver
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
        pulverizeByItemId.clear()
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
