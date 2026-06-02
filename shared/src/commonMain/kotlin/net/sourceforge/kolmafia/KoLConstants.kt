package net.sourceforge.kolmafia

import kotlin.random.Random

object KoLConstants {
    val RNG = Random.Default

    val LINE_BREAK_PATTERN = Regex("[\r\n]+")
    val ANYTAG_PATTERN = Regex("<[^>]*>")
    val SCRIPT_PATTERN = Regex("(?is)<script[^>]*>.*?</script>")
    val COMMENT_PATTERN = Regex("(?s)<!--.*?-->")
}
