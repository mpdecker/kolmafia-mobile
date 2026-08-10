package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.character.Beeosity
import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Parses items.txt from the bundled compose resources.
// Format (tab-separated): id  name  descid  image  use  access  autosell  [plural]
// Call load() once at app startup (or lazily on first access).
@OptIn(ExperimentalResourceApi::class)
object ItemDatabase {

    private val byId = mutableMapOf<Int, ItemData>()
    private val byName = mutableMapOf<String, ItemData>()
    private val byPlural = mutableMapOf<String, ItemData>()
    private val byDescId = mutableMapOf<String, ItemData>()
    private val noobSkillIdByItemId = mutableMapOf<Int, Int>()
    private val itemIdsByNoobSkillId = mutableMapOf<Int, MutableList<Int>>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/items.txt").decodeToString()
        parse(text)
        loaded = true
    }

    fun getById(id: Int): ItemData? = byId[id]
    fun getByName(name: String): ItemData? = byName[name.lowercase()]
    fun getByDescId(descId: String): ItemData? = byDescId[descId]
    fun getByPluralOrName(name: String): ItemData? {
        val lower = name.lowercase()
        return byName[lower] ?: byPlural[lower]
    }
    fun all(): Collection<ItemData> = byId.values

    /** Desktop ItemDatabase.maxItemId — highest bundled item id, or 0 if empty. */
    fun maxItemId(): Int = byId.keys.maxOrNull() ?: 0

    fun isTradeable(itemId: Int): Boolean = getById(itemId)?.isTradeable ?: false

    fun isGiftable(itemId: Int): Boolean = getById(itemId)?.isGiftable ?: false

    fun isDiscardable(itemId: Int): Boolean = getById(itemId)?.isDiscardable ?: false

    fun isQuestItem(itemId: Int): Boolean = getById(itemId)?.isQuestItem ?: false

    fun isDisplayable(itemId: Int): Boolean =
        itemId > 0 && !isQuestItem(itemId) && !isVirtualItem(itemId)

    /** Desktop ItemDatabase.isGiftItem — GIFT access flag only (not isGiftable). */
    fun isGiftItem(itemId: Int): Boolean = getById(itemId)?.access?.contains('g') == true

    fun isUsable(itemId: Int): Boolean {
        val item = getById(itemId) ?: return false
        return when (item.primaryUse) {
            ItemPrimaryUse.USABLE,
            ItemPrimaryUse.MULTIPLE,
            ItemPrimaryUse.REUSABLE,
            ItemPrimaryUse.POTION,
            ItemPrimaryUse.AVATAR,
            ItemPrimaryUse.GROW,
            -> true
            else -> item.hasSecondary("usable")
        }
    }

    fun isMultiUsable(itemId: Int): Boolean {
        val item = getById(itemId) ?: return false
        return item.primaryUse == ItemPrimaryUse.MULTIPLE || item.hasSecondary("multiple")
    }

    fun isReusable(itemId: Int): Boolean {
        val item = getById(itemId) ?: return false
        return item.primaryUse == ItemPrimaryUse.REUSABLE
            || item.hasSecondary("reusable", "combat reusable")
    }

    fun isCombatUsable(itemId: Int): Boolean =
        getById(itemId)?.hasSecondary("combat", "combat reusable") == true

    fun isCombatReusable(itemId: Int): Boolean =
        getById(itemId)?.hasSecondary("combat reusable") == true

    fun isFancyItem(itemId: Int): Boolean =
        getById(itemId)?.hasSecondary("fancy") == true

    fun isPasteable(itemId: Int): Boolean =
        getById(itemId)?.hasSecondary("paste") == true

    fun isSmithable(itemId: Int): Boolean =
        getById(itemId)?.hasSecondary("smith") == true

    fun isCookable(itemId: Int): Boolean =
        getById(itemId)?.hasSecondary("cook") == true

    fun isMixable(itemId: Int): Boolean =
        getById(itemId)?.hasSecondary("mix") == true

    fun isPotion(itemId: Int): Boolean {
        val item = getById(itemId) ?: return false
        return item.primaryUse == ItemPrimaryUse.POTION || item.primaryUse == ItemPrimaryUse.AVATAR
    }

    fun isChocolateItem(itemId: Int): Boolean =
        getById(itemId)?.hasSecondary("chocolate") == true

    fun isCandyItem(itemId: Int): Boolean =
        getById(itemId)?.hasSecondary("candy", "candy1", "candy2") == true

    fun getCandyTypeName(itemId: Int): String {
        val item = getById(itemId) ?: return ""
        return when {
            item.hasSecondary("candy2") -> "complex"
            item.hasSecondary("candy1") -> "simple"
            item.hasSecondary("candy") -> "unspaded"
            else -> ""
        }
    }

    fun getNameLength(itemId: Int): Int = getById(itemId)?.name?.length ?: 0

    fun getItemName(itemId: Int): String = getById(itemId)?.name ?: ""

    /** Desktop ItemDatabase.unusableInBeecore — beeosity gate with explicit usable exceptions. */
    fun unusableInBeecore(itemId: Int): Boolean =
        when (itemId) {
            in BEECORE_USABLE_ITEM_IDS -> false
            else -> Beeosity.hasBeeosity(getItemName(itemId))
        }

    /** Desktop ItemDatabase.unusableInGLover — G-lessness gate with explicit exceptions. */
    fun unusableInGLover(itemId: Int): Boolean =
        when (itemId) {
            in GLOVER_USABLE_ITEM_IDS -> false
            else -> !Beeosity.hasGs(getItemName(itemId))
        }

    const val BALL_POLISH = 2964
    const val FRATHOUSE_BLUEPRINTS = 2951
    const val BINDER_CLIP = 6694
    const val ICE_BABY = 1425
    const val JUGGLERS_BALLS = 2223
    const val EYEBALL_PENDANT = 2226
    const val SPOOKY_PUTTY_BALL = 3664
    const val LOATHING_LEGION_ABACUS = 4923
    const val LOATHING_LEGION_DEFIBRILLATOR = 4919
    const val LOATHING_LEGION_DOUBLE_PRISM = 4920
    const val LOATHING_LEGION_ROLLERBLADES = 4916
    const val COBBS_KNOB_MAP = 2442
    const val ENCRYPTION_KEY = 2441
    const val ENCHANTED_BEAN = 186
    const val GONG = 3353
    const val ASTRAL_MUSHROOM = 1622

    fun getImage(itemId: Int): String = getById(itemId)?.image ?: ""

    /** Desktop ItemDatabase.getSmallImage — 30x30 folder images for Folder Holder folders. */
    fun getSmallImage(itemId: Int): String = when (itemId) {
        in FOLDER_SMALL_IMAGE_2 -> "folder2.gif"
        in FOLDER_SMALL_IMAGE_1 -> "folder1.gif"
        else -> getImage(itemId)
    }

    /** Desktop ItemDatabase.getNoobSkillId — noobcore skill granted by absorbing this item. */
    fun getNoobSkillId(itemId: Int): Int = noobSkillIdByItemId[itemId] ?: 0

    /** Desktop ItemDatabase.getItemListByNoobSkillId — items that grant a noobcore skill when absorbed. */
    fun getItemListByNoobSkillId(skillId: Int): IntArray =
        itemIdsByNoobSkillId[skillId]?.toIntArray() ?: IntArray(0)

    /** Virtual items exist in KoL data but cannot live in inventory (desktop ItemDatabase.isVirtualItem). */
    fun isVirtualItem(itemId: Int): Boolean = itemId in VIRTUAL_ITEM_IDS

    fun getPluralName(itemId: Int): String {
        if (itemId <= 0) return ""
        val item = getById(itemId) ?: return ""
        val plural = item.plural?.takeIf { it.isNotBlank() }
        return plural ?: "${item.name}s"
    }

    /** Test hook — register an item without loading items.txt. */
    internal fun registerForTest(item: ItemData) {
        byId[item.id] = item
        byName[item.name.lowercase()] = item
        byDescId[item.descId] = item
        item.plural?.let { byPlural[it.lowercase()] = item }
        registerNoobSkillId(item)
    }

    internal fun resetForTest() {
        byId.clear()
        byName.clear()
        byPlural.clear()
        byDescId.clear()
        noobSkillIdByItemId.clear()
        itemIdsByNoobSkillId.clear()
        loaded = false
    }

    private fun parse(text: String) {
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            // Skip version line (first non-comment line is just a number)
            val firstToken = line.substringBefore('\t')
            if (firstToken.toIntOrNull() != null && line.count { it == '\t' } < 3) continue

            val parts = line.split('\t')
            if (parts.size < 7) continue

            val id = parts[0].toIntOrNull() ?: continue
            val name = parts[1]
            val descId = parts[2]
            val image = parts[3]
            val useParts = parts[4].split(',')
            val primaryUse = ItemPrimaryUse.fromString(useParts.firstOrNull()?.trim() ?: "none")
            val secondaryUses = useParts.drop(1).map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            val accessStr = parts[5]
            val access = accessStr.split(',').mapNotNull { it.trim().firstOrNull() }.toSet()
            val autosell = parts[6].toIntOrNull() ?: 0
            val plural = parts.getOrNull(7)?.takeIf { it.isNotBlank() }

            val item = ItemData(id, name, descId, image, primaryUse, secondaryUses, access, autosell, plural)
            byId[id] = item
            byName[name.lowercase()] = item
            byDescId[descId] = item
            plural?.let { byPlural[it.lowercase()] = item }
            registerNoobSkillId(item)
        }
    }

    private fun registerNoobSkillId(item: ItemData) {
        if (!qualifiesForNoobSkill(item)) return
        val skillId = robortenderNoobSkillId(item.id)
            ?: ((item.descId.toIntOrNull() ?: 0) % 125 + 23001)
        noobSkillIdByItemId[item.id] = skillId
        itemIdsByNoobSkillId.getOrPut(skillId) { mutableListOf() }.add(item.id)
    }

    private fun qualifiesForNoobSkill(item: ItemData): Boolean {
        if (item.isQuestItem || !item.isDiscardable) return false
        if (item.isEquipment && item.primaryUse != ItemPrimaryUse.FAMILIAR) return false
        return item.isTradeable ||
            isGiftItem(item.id) ||
            item.id in NOOB_SKILL_SPECIAL_ITEM_IDS
    }

    private fun robortenderNoobSkillId(itemId: Int): Int? = when (itemId) {
        9349 -> 23304 // novelty hot sauce -> Frown Muscles
        9353 -> 23302 // cocktail mushroom -> Retractable Toes
        9354 -> 23303 // granola liqueur -> Ink Gland
        9357 -> 23301 // gregnadigne -> Bendable Knees
        9359 -> 23306 // baby oil shooter -> Powerful Vocal Chords
        9361 -> 23305 // limepatch -> Anger Glands
        else -> null
    }

    private fun ItemData.hasSecondary(vararg tags: String): Boolean =
        tags.any { tag -> secondaryUses.any { it.equals(tag, ignoreCase = true) } }

    private val NOOB_SKILL_SPECIAL_ITEM_IDS = setOf(
        9216, // clod of dirt
        9343, // dirty bottlecap
        9344, // discarded button
    )

    private val FOLDER_SMALL_IMAGE_2 = setOf(
        6618, 6619, 6620, 6621, 6622, 6624, 6626, 6627, 6629, 6630, 6641, 6643,
    )

    private val FOLDER_SMALL_IMAGE_1 = setOf(
        6623, 6625, 6628, 6631, 6632, 6633, 6634, 6635, 6636, 6637, 6638, 6639, 6640, 6642, 6644, 6645,
    )

    private val BEECORE_USABLE_ITEM_IDS = setOf(
        BALL_POLISH,
        FRATHOUSE_BLUEPRINTS,
        COBBS_KNOB_MAP,
        BINDER_CLIP,
        ICE_BABY,
        JUGGLERS_BALLS,
        EYEBALL_PENDANT,
        SPOOKY_PUTTY_BALL,
        LOATHING_LEGION_ABACUS,
        LOATHING_LEGION_DEFIBRILLATOR,
        LOATHING_LEGION_DOUBLE_PRISM,
        LOATHING_LEGION_ROLLERBLADES,
        ENCHANTED_BEAN,
    )

    private val GLOVER_USABLE_ITEM_IDS = setOf(
        COBBS_KNOB_MAP,
        ENCHANTED_BEAN,
        7262, // palindrome book 1
        7270, // palindrome book 2
    )

    private val VIRTUAL_ITEM_IDS = setOf(
        3649, // madness reef map
        3683, // marinara trench map
        3701, // anemone mine map
        3774, // dive bar map
        4222, // skate park map
        7589, // glass of milk
        7590, // cup of tea
        7591, // thermos of whiskey
        7592, // lucky lindy
        7593, // bee's knees
        7594, // sockdolloger
        7595, // ish kabibble
        7596, // hot socks
        7597, // phonus balonus
        7598, // flivver
        7599, // sloppy jalopy
    )
}
