package net.sourceforge.kolmafia.utilities

/** Desktop [StringUtilities.leetify]. */
fun leetify(text: String): String {
    val builder = StringBuilder()
    for (char in text) {
        when (char) {
            'O', 'o' -> builder.append('0')
            'I', 'i', 'L', 'l' -> builder.append('1')
            'E', 'e' -> builder.append('3')
            'A', 'a' -> builder.append('4')
            'S', 's' -> builder.append('5')
            'T', 't' -> builder.append('7')
            else -> builder.append(char)
        }
    }
    return builder.toString()
}
