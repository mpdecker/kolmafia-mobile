package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.clan.ClanManager

object ClanHallRequest {
    private val CLAN_NAME_PATTERN = Regex(
        """<center><b>(.*?)</b>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun parseResponse(url: String, html: String) {
        if (!url.contains("clan_hall.php", ignoreCase = true)) return
        val name = CLAN_NAME_PATTERN.find(html)?.groupValues?.get(1)
            ?.replace(Regex("<.*?>"), "")?.trim().orEmpty()
        if (name.isNotEmpty() && name != ClanManager.getClanName()) {
            ClanManager.resetClanId()
        }
    }
}
