package net.sourceforge.kolmafia.volcano

class VolcanoPath private constructor(private val hops: List<Int>) : Iterable<Int> {
    constructor(square: Int) : this(listOf(square))

    constructor(prefix: VolcanoPath, square: Int) : this(prefix.hops + square)

    fun size(): Int = hops.size

    operator fun get(index: Int): Int = hops[index]

    fun getLast(): Int = hops.last()

    fun contains(elem: Int): Boolean = elem in hops

    override fun iterator(): Iterator<Int> = hops.iterator()

    override fun toString(): String = hops.joinToString(prefix = "[", postfix = "]")
}
