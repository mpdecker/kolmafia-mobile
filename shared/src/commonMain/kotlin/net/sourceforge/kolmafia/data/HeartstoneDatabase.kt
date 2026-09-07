package net.sourceforge.kolmafia.data

/**
 * Desktop [HeartstoneDatabase] — middle-letter / UTF-8 length helpers for Heartstone Attunement.
 */
object HeartstoneDatabase {
    data class MiddleLetter(val letter: String, val byteIndex: Int)

    fun spaceStrippedStringLength(str: String): Int {
        val compact = str.replace(Regex("""\s+"""), "")
        return compact.encodeToByteArray().size
    }

    fun middleLetter(monsterName: String): MiddleLetter? {
        if (monsterName.isEmpty()) return null
        val compact = monsterName.replace(Regex("""\s+"""), "")
        val bytes = compact.encodeToByteArray()
        val length = bytes.size
        // even length has no middle
        if (length % 2 == 0) return null
        val mid = length / 2
        val middle = bytes.copyOfRange(mid, mid + 1).decodeToString()
        if (!middle.matches(Regex("""^[A-Za-z]$"""))) return null
        return MiddleLetter(middle.uppercase(), mid)
    }
}
