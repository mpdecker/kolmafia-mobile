package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.craftMode
import net.sourceforge.kolmafia.data.isClipArtCraftable
import net.sourceforge.kolmafia.data.isCoinmasterCraftable
import net.sourceforge.kolmafia.data.isCreateSupported
import net.sourceforge.kolmafia.data.isBarrelCraftable
import net.sourceforge.kolmafia.data.isJewelCraftable
import net.sourceforge.kolmafia.data.isMalusCraftable
import net.sourceforge.kolmafia.data.isMuseCraftable
import net.sourceforge.kolmafia.data.isPhineasCraftable
import net.sourceforge.kolmafia.data.isStaffCraftable
import net.sourceforge.kolmafia.data.isSushiCraftable
import net.sourceforge.kolmafia.data.isTinkerCraftable
import net.sourceforge.kolmafia.data.isRollCraftable
import net.sourceforge.kolmafia.data.isSewerCraftable
import net.sourceforge.kolmafia.data.isStationCraftable
import net.sourceforge.kolmafia.data.isStillCraftable
import net.sourceforge.kolmafia.data.isSuseCraftable
import net.sourceforge.kolmafia.data.isTerminalCraftable
import net.sourceforge.kolmafia.data.isVykeaCraftable
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterManager
import net.sourceforge.kolmafia.shop.ShopRequest

/** Desktop CreateItemRequest v13 — method router for station/SUSE/STILL/COINMASTER/CLIPART/ROLL/TERMINAL/SEWER/VYKEA/MUSE/PHINEAS/STAFF/GNOME_TINKER/SUSHI/MALUS/JEWEL/BARREL create. */
class ConcoctionCreateRequest(
    private val retrieveItemService: RetrieveItemService?,
    private val craftRequest: CraftRequest?,
    private val useItemRequest: UseItemRequest?,
    private val gameDatabase: GameDatabase?,
    private val createItemIngredients: CreateItemIngredients? = null,
    private val shopRequest: ShopRequest? = null,
    private val coinmasterManager: CoinmasterManager? = null,
    private val character: KoLCharacter? = null,
    private val clipArtCreateRequest: ClipArtCreateRequest? = null,
    private val rollingPinCreateRequest: RollingPinCreateRequest? = null,
    private val terminalExtrudeCreateRequest: TerminalExtrudeCreateRequest? = null,
    private val sewerCreateRequest: SewerCreateRequest? = null,
    private val vykeaCreateRequest: VykeaCreateRequest? = null,
    private val museCreateRequest: MuseCreateRequest? = null,
    private val phineasCreateRequest: PhineasCreateRequest? = null,
    private val staffCreateRequest: StaffCreateRequest? = null,
    private val gnomeTinkerCreateRequest: GnomeTinkerCreateRequest? = null,
    private val sushiCreateRequest: SushiCreateRequest? = null,
    private val malusCreateRequest: MalusCreateRequest? = null,
    private val jewelCreateRequest: JewelCreateRequest? = null,
    private val barrelCreateRequest: BarrelCreateRequest? = null,
) {
    private val stillCreateRequest = StillCreateRequest(
        shopRequest = shopRequest,
        createItemIngredients = createItemIngredients,
        gameDatabase = gameDatabase,
        character = character,
    )

    suspend fun create(
        concoctionName: String,
        quantity: Int,
        state: CharacterState? = null,
        preferences: Preferences? = null,
    ): Result<Int> {
        if (quantity <= 0) return Result.success(0)
        val concoction = ConcoctionDatabase.getByResult(concoctionName)
            ?: return Result.failure(IllegalStateException("No concoction for: $concoctionName"))
        if (!concoction.isCreateSupported()) {
            return Result.failure(IllegalStateException("Concoction create not supported: $concoctionName"))
        }

        val retrieve = retrieveItemService
            ?: return Result.failure(IllegalStateException("RetrieveItemService not configured"))

        return try {
            val created = when {
                concoction.isClipArtCraftable() ->
                    clipArtCreateRequest?.create(concoction, quantity, state, preferences)?.getOrThrow() ?: 0
                concoction.isRollCraftable() ->
                    rollingPinCreateRequest?.create(concoction, quantity)?.getOrThrow() ?: 0
                concoction.isTerminalCraftable() ->
                    terminalExtrudeCreateRequest?.create(concoction, quantity, state, preferences)?.getOrThrow() ?: 0
                concoction.isSewerCraftable() ->
                    sewerCreateRequest?.create(concoction, quantity, state, preferences)?.getOrThrow() ?: 0
                concoction.isVykeaCraftable() ->
                    vykeaCreateRequest?.create(concoction, quantity, state, preferences)?.getOrThrow() ?: 0
                concoction.isMuseCraftable() ->
                    museCreateRequest?.create(concoction, quantity, state, preferences)?.getOrThrow() ?: 0
                concoction.isPhineasCraftable() ->
                    phineasCreateRequest?.create(concoction, quantity, state, preferences)?.getOrThrow() ?: 0
                concoction.isStaffCraftable() ->
                    staffCreateRequest?.create(concoction, quantity, state, preferences)?.getOrThrow() ?: 0
                concoction.isTinkerCraftable() ->
                    gnomeTinkerCreateRequest?.create(concoction, quantity, state, preferences)?.getOrThrow() ?: 0
                concoction.isSushiCraftable() ->
                    sushiCreateRequest?.create(concoction, quantity, state, preferences)?.getOrThrow() ?: 0
                concoction.isMalusCraftable() ->
                    malusCreateRequest?.create(concoction, quantity, state, preferences)?.getOrThrow() ?: 0
                concoction.isJewelCraftable() ->
                    jewelCreateRequest?.create(concoction, quantity, state, preferences)?.getOrThrow() ?: 0
                concoction.isBarrelCraftable() ->
                    barrelCreateRequest?.create(concoction, quantity, state, preferences)?.getOrThrow() ?: 0
                concoction.isStillCraftable() ->
                    stillCreateRequest.create(concoction, quantity, state, preferences).getOrThrow()
                concoction.isCoinmasterCraftable() ->
                    createCoinmaster(concoction, quantity)
                concoction.isSuseCraftable() ->
                    createSuse(concoction, quantity, state)
                concoction.isStationCraftable() ->
                    createStation(concoction, quantity, state)
                else -> 0
            }
            if (created < quantity) {
                Result.failure(IllegalStateException("Could not create $quantity of $concoctionName (got $created)"))
            } else {
                Result.success(created)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun createCoinmaster(concoction: ConcoctionData, quantity: Int): Int {
        val coinmaster = coinmasterManager ?: return 0
        val itemId = gameDatabase?.item(concoction.result)?.id
            ?: ItemDatabase.getByName(concoction.result)?.id
            ?: return 0
        return coinmaster.buyItem(itemId, quantity)
    }

    private suspend fun createSuse(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
    ): Int {
        val use = useItemRequest ?: return 0
        val helper = createItemIngredients ?: return 0
        val sourceName = concoction.ingredients.firstOrNull()?.name ?: return 0
        val sourceId = gameDatabase?.item(sourceName)?.id ?: ItemDatabase.getByName(sourceName)?.id ?: return 0
        var created = 0
        repeat(quantity) {
            if (!helper.makeIngredients(concoction, 1, state)) return created
            if (use.use(sourceId, 1).isFailure) return created
            created++
        }
        return created
    }

    private suspend fun createStation(
        concoction: ConcoctionData,
        quantity: Int,
        state: CharacterState?,
    ): Int {
        val mode = concoction.craftMode()?.apiAction ?: return 0
        val craft = craftRequest ?: return 0
        val helper = createItemIngredients ?: return 0
        val ing1Name = concoction.ingredients.getOrNull(0)?.name ?: return 0
        val ing2Name = concoction.ingredients.getOrNull(1)?.name ?: return 0
        val ing1 = gameDatabase?.item(ing1Name)?.id ?: ItemDatabase.getByName(ing1Name)?.id ?: return 0
        val ing2 = gameDatabase?.item(ing2Name)?.id ?: ItemDatabase.getByName(ing2Name)?.id ?: return 0

        var created = 0
        var remaining = quantity
        while (remaining > 0) {
            val batch = remaining
            if (!helper.makeIngredients(concoction, batch, state)) {
                return created
            }
            val crafted = craft.craft(mode, batch, ing1, ing2)
            if (crafted <= 0) break
            val gained = crafted.coerceAtMost(remaining)
            created += gained
            remaining -= gained
        }
        return created
    }
}
