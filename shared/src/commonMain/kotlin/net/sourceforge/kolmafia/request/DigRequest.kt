package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.session.EquipmentManager
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [net.sourceforge.kolmafia.request.DigRequest] desert archaeology. */
object DigRequest {
    const val ARCHAEOLOGING_SHOVEL = 4702
    private val SQUARE = Regex("""s1=(\d+)&s2=(\d+)&s3=(\d+)""")

    fun parseResponse(url: String, html: String, equipmentManager: EquipmentManager?) {
        if (!url.contains("dig.php", ignoreCase = true)) return
        if (html.contains("Your archaeologing shovel can't take any more abuse.", ignoreCase = true)) {
            equipmentManager?.breakEquipment(ARCHAEOLOGING_SHOVEL)
        }
    }

    fun registerRequest(url: String, sessionLogger: SessionLogger?): Boolean {
        if (!url.contains("dig.php", ignoreCase = true)) return false
        val action = Regex("""action=([^&]+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1)
        if (action.equals("dig", ignoreCase = true)) {
            val squares = SQUARE.find(url)
            val message = if (squares != null) {
                "Digging at ${squares.groupValues[1]}, ${squares.groupValues[2]}, ${squares.groupValues[3]}"
            } else {
                "Digging"
            }
            sessionLogger?.appendRawLine(message)
        }
        return true
    }
}
