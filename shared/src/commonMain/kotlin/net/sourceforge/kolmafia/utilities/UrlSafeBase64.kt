package net.sourceforge.kolmafia.utilities

/** URL-safe Base64 without padding (RFC 4648 §5) for KMP common code. */
object UrlSafeBase64 {
    private val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray()

    fun encodeWithoutPadding(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val out = StringBuilder((bytes.size * 4 + 2) / 3)
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val has1 = i + 1 < bytes.size
            val has2 = i + 2 < bytes.size
            val b1 = if (has1) bytes[i + 1].toInt() and 0xFF else 0
            val b2 = if (has2) bytes[i + 2].toInt() and 0xFF else 0
            val n = (b0 shl 16) or (b1 shl 8) or b2
            out.append(ALPHABET[(n ushr 18) and 63])
            out.append(ALPHABET[(n ushr 12) and 63])
            if (has1) out.append(ALPHABET[(n ushr 6) and 63])
            if (has2) out.append(ALPHABET[n and 63])
            i += 3
        }
        return out.toString()
    }
}
