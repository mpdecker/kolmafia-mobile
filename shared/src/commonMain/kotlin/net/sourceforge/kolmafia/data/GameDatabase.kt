package net.sourceforge.kolmafia.data

// Coordinates loading of all bundled game data files.
// Inject this singleton via Koin and call load() once at session start.
// Individual databases are also accessible as object singletons for direct use.
open class GameDatabase {

    var isLoaded = false
        private set

    suspend fun load() {
        if (isLoaded) return

        // Core item / effect / equipment data
        ItemDatabase.load()
        EffectDatabase.load()
        EquipmentDatabase.load()

        // Skills, familiars, outfits, bounties
        SkillDefinitionDatabase.load()
        FamiliarDefinitionDatabase.load()
        OutfitDatabase.load()
        BountyDatabase.load()

        // Consumables (food / drink / spleen) and cafe menu
        ConsumableDatabase.load()
        CafeDatabase.load()

        // Restores and modifiers
        RestoreDatabase.load()
        ModifierDatabase.load()

        // Adventure zones and combat data
        AdventureDatabase.load()
        ZoneParentDatabase.load()
        CombatDatabase.load()
        EncounterDatabase.load()

        // Monsters and crafting
        MonsterDatabase.load()
        ConcoctionDatabase.load()

        // Item relationships and shop data
        ZapGroupDatabase.load()
        FoldGroupDatabase.load()
        PackageDatabase.load()
        NpcStoreDatabase.load()
        DailyLimitDatabase.load()

        // Quest log text patterns (for quest state detection)
        QuestLogDatabase.load()

        isLoaded = true
    }

    // Convenience accessors — delegates to the respective object singletons.
    open fun item(id: Int): ItemData? = ItemDatabase.getById(id)
    open fun item(name: String): ItemData? = ItemDatabase.getByName(name)
    open fun effect(id: Int) = EffectDatabase.getById(id)
    open fun effect(name: String) = EffectDatabase.getByName(name)
    fun equipment(name: String) = EquipmentDatabase.getByName(name)
    open fun skill(id: Int) = SkillDefinitionDatabase.getById(id)
    open fun skill(name: String) = SkillDefinitionDatabase.getByName(name)
    open fun familiar(id: Int) = FamiliarDefinitionDatabase.getById(id)
    open fun familiar(name: String) = FamiliarDefinitionDatabase.getByName(name)
    fun outfit(id: Int) = OutfitDatabase.getById(id)
    fun outfit(name: String) = OutfitDatabase.getByName(name)
    fun bounty(name: String) = BountyDatabase.getByName(name)
    fun food(name: String) = ConsumableDatabase.getFood(name)
    fun drink(name: String) = ConsumableDatabase.getDrink(name)
    fun spleen(name: String) = ConsumableDatabase.getSpleen(name)
    fun restore(name: String) = RestoreDatabase.getByName(name)
    fun modifier(type: String, name: String) = ModifierDatabase.get(type, name)
    fun itemModifier(name: String) = ModifierDatabase.getItem(name)
    fun effectModifier(name: String) = ModifierDatabase.getEffect(name)
    open fun zone(locationName: String) = AdventureDatabase.getByName(locationName)
    open fun monster(id: Int) = MonsterDatabase.getById(id)
    open fun monster(name: String) = MonsterDatabase.getByName(name)
    fun recipe(resultName: String) = ConcoctionDatabase.getByResult(resultName)
    fun recipesUsing(ingredientName: String) = ConcoctionDatabase.getByIngredient(ingredientName)
    fun zapGroup(itemName: String) = ZapGroupDatabase.groupFor(itemName)
    fun foldGroup(itemName: String) = FoldGroupDatabase.groupFor(itemName)
    fun npcStore(key: String) = NpcStoreDatabase.getByKey(key)
    open fun npcPrice(itemName: String): Int = NpcStoreDatabase.npcPrice(itemName)
    open fun npcStoreFor(itemName: String): NpcStoreData? = NpcStoreDatabase.storeForItem(itemName)
    fun dailyLimit(name: String) = DailyLimitDatabase.getByName(name)
    fun packageItem(name: String) = PackageDatabase.getByName(name)
    fun cafeFood(name: String) = CafeDatabase.getFood(name)
    fun cafeDrink(name: String) = CafeDatabase.getDrink(name)
}
