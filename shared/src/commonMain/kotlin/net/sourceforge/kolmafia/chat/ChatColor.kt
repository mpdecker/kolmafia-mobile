package net.sourceforge.kolmafia.chat

import androidx.compose.ui.graphics.Color

/** Map desktop HTML color names from `chat_notify` to Compose colors. */
fun parseChatColor(name: String?): Color? {
    if (name.isNullOrBlank()) return null
    return when (name.trim().trim('"').lowercase()) {
        "red" -> Color.Red
        "green" -> Color.Green
        "blue" -> Color.Blue
        "orange" -> Color(0xFFFFA500)
        "yellow" -> Color.Yellow
        "purple" -> Color(0xFF800080)
        "gray", "grey" -> Color.Gray
        else -> null
    }
}
