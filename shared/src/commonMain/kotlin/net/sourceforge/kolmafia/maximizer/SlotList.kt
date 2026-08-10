package net.sourceforge.kolmafia.maximizer

/**
 * Per-slot ranked candidate buckets, mirroring desktop [net.sourceforge.kolmafia.maximizer.SlotList].
 */
class SlotList<T> {
    private val slotList = mutableMapOf<MaximizerSlot, MutableList<T>>()

    fun get(slot: MaximizerSlot): MutableList<T> =
        slotList.getOrPut(slot) { mutableListOf() }

    fun set(slot: MaximizerSlot, value: List<T>) {
        slotList[slot] = value.toMutableList()
    }

    fun entries(): List<Pair<MaximizerSlot, List<T>>> =
        slotList.map { (slot, items) -> slot to items.toList() }

    fun sortedDescending(slot: MaximizerSlot, selector: (T) -> Double): List<T> =
        get(slot).sortedByDescending(selector)

    fun allItems(slot: MaximizerSlot): List<T> = get(slot).toList()

    fun isEmpty(slot: MaximizerSlot): Boolean = get(slot).isEmpty()
}
