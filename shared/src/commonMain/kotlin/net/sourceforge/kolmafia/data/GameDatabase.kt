package net.sourceforge.kolmafia.data

// Coordinates loading of all bundled game data files.
// Inject this singleton and call load() once at session start.
class GameDatabase {

    var isLoaded = false
        private set

    suspend fun load() {
        if (isLoaded) return
        ItemDatabase.load()
        EffectDatabase.load()
        EquipmentDatabase.load()
        isLoaded = true
    }

    fun item(id: Int) = ItemDatabase.getById(id)
    fun item(name: String) = ItemDatabase.getByName(name)
    fun effect(id: Int) = EffectDatabase.getById(id)
    fun effect(name: String) = EffectDatabase.getByName(name)
    fun equipment(name: String) = EquipmentDatabase.getByName(name)
}
