package net.sourceforge.kolmafia.data

/** Desktop EquipmentDatabase pulverization bitmask flags. */
object PulverizeFlags {
    const val PULVERIZE_BITS = 0x80000000.toInt()

    const val YIELD_UNCERTAIN = 0x001
    const val YIELD_1P = 0x002
    const val YIELD_2P = 0x004
    const val YIELD_3P = 0x008
    const val YIELD_4P_1N = 0x010
    const val YIELD_1N3P_2N = 0x020
    const val YIELD_3N = 0x040
    const val YIELD_4N_1W = 0x080
    const val YIELD_1W3N_2W = 0x100
    const val YIELD_3W = 0x200
    const val YIELD_1C = 0x400

    const val ELEM_TWINKLY = 0x01000
    const val ELEM_HOT = 0x02000
    const val ELEM_COLD = 0x04000
    const val ELEM_STENCH = 0x08000
    const val ELEM_SPOOKY = 0x10000
    const val ELEM_SLEAZE = 0x20000
    const val ELEM_OTHER = 0x40000

    const val MALUS_UPGRADE = 0x100000
}
