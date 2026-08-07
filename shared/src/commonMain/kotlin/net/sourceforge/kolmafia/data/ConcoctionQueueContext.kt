package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.item.FreeCraftingTurns
import net.sourceforge.kolmafia.preferences.Preferences

/** Context for desktop Concoction.queue budget reservation during craft queue push. */
data class ConcoctionQueueContext(
    val freeCrafting: FreeCraftingTurns.Context = FreeCraftingTurns.Context(),
    val availableCountById: (Int) -> Int = { 0 },
    val getBooleanPref: (String) -> Boolean = { key -> DefaultsDatabase.getBoolean(key) },
    val getIntPref: (String) -> Int = { key -> DefaultsDatabase.getInt(key) },
    val getStringPref: (String) -> String = { key -> DefaultsDatabase.getString(key) },
    val preferences: Preferences? = null,
    val foodQueueDepth: () -> Int = { 0 },
    val isFancyDog: (String) -> Boolean = HotDogDatabase::isFancyHotDog,
    val isPermitted: (ConcoctionData) -> Boolean = { true },
    val runtimeFor: (String) -> ConcoctionRuntimeState? = { null },
) {
    fun booleanPref(key: String): Boolean =
        preferences?.getBoolean(key) ?: getBooleanPref(key)

    fun intPref(key: String): Int =
        preferences?.getInt(key) ?: getIntPref(key)

    companion object {
        fun fromRefreshContext(context: ConcoctionRefreshContext): ConcoctionQueueContext {
            val foodDepth = {
                ConcoctionCraftQueue.depth(ConcoctionOrganAmounts.QueueBucket.FOOD)
            }
            return ConcoctionQueueContext(
                availableCountById = context.availableCountById,
                isPermitted = context.isPermitted,
                preferences = context.preferences,
                getBooleanPref = { key ->
                    context.preferences?.getBoolean(key)
                        ?: DefaultsDatabase.getBoolean(key)
                },
                getIntPref = { key ->
                    context.preferences?.getInt(key)
                        ?: DefaultsDatabase.getInt(key)
                },
                foodQueueDepth = foodDepth,
                isFancyDog = HotDogDatabase::isFancyHotDog,
                runtimeFor = { name -> ConcoctionDatabase.getRuntime(name) },
            )
        }
    }
}
