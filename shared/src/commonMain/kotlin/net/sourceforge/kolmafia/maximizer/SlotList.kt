package net.sourceforge.kolmafia.maximizer

/**
 * Per-slot ranked candidate buckets, mirroring desktop [net.sourceforge.kolmafia.maximizer.SlotList].
 * Phase 375: indexed familiar buckets via [getFamiliar] for switch-familiar carry equipment.
 */
class SlotList<T>(familiarCount: Int = 0) {
    private val slotList = mutableMapOf<MaximizerSlot, MutableList<T>>()
    private val familiarList = List(familiarCount) { mutableListOf<T>() }

    fun get(slot: MaximizerSlot): MutableList<T> =
        slotList.getOrPut(slot) { mutableListOf() }

    fun getFamiliar(index: Int): MutableList<T> = familiarList[index]

    fun familiarCount(): Int = familiarList.size

    fun set(slot: MaximizerSlot, value: List<T>) {
        slotList[slot] = value.toMutableList()
    }

    fun setFamiliar(index: Int, value: List<T>) {
        familiarList[index].clear()
        familiarList[index].addAll(value)
    }

    /** Slot-only entries (excludes per-familiar buckets). */
    fun slotEntries(): List<Pair<MaximizerSlot, List<T>>> =
        slotList.map { (slot, items) -> slot to items.toList() }

    /** All slot buckets plus per-familiar buckets. */
    fun entries(): List<SlotListEntry<T>> = buildList {
        for ((slot, items) in slotList) {
            add(SlotListEntry.Slot(slot, items.toList()))
        }
        for (index in familiarList.indices) {
            add(SlotListEntry.Familiar(index, familiarList[index].toList()))
        }
    }

    fun sortedDescending(slot: MaximizerSlot, selector: (T) -> Double): List<T> =
        get(slot).sortedByDescending(selector)

    fun allItems(slot: MaximizerSlot): List<T> = get(slot).toList()

    fun isEmpty(slot: MaximizerSlot): Boolean = get(slot).isEmpty()
}

sealed class SlotListEntry<out T> {
    data class Slot<T>(val slot: MaximizerSlot, val items: List<T>) : SlotListEntry<T>()
    data class Familiar<T>(val index: Int, val items: List<T>) : SlotListEntry<T>()
}
