package net.sourceforge.kolmafia.request

/** Desktop [net.sourceforge.kolmafia.request.EatItemRequest] inv_eat.php alias hub. */
object EatItemRequest {
    fun registerRequest(url: String): Boolean = url.contains("inv_eat.php", ignoreCase = true)
}

/** Desktop [net.sourceforge.kolmafia.request.DrinkItemRequest] inv_booze.php alias hub. */
object DrinkItemRequest {
    fun registerRequest(url: String): Boolean = url.contains("inv_booze.php", ignoreCase = true)
}

/** Desktop [net.sourceforge.kolmafia.request.SpleenItemRequest] inv_spleen.php alias hub. */
object SpleenItemRequest {
    fun registerRequest(url: String): Boolean = url.contains("inv_spleen.php", ignoreCase = true)
}

/** Desktop [net.sourceforge.kolmafia.request.ApiRequest] api.php status hub. */
object ApiRequest {
    fun registerRequest(url: String): Boolean = url.contains("api.php", ignoreCase = true)
}

/** Desktop [net.sourceforge.kolmafia.request.ContactListRequest]. */
object ContactListRequest {
    fun parseResponse(url: String, html: String) {
        if (!url.contains("account_contactlist.php", ignoreCase = true)) return
        net.sourceforge.kolmafia.session.ContactManager.updateFromHtml(html)
    }

    fun registerRequest(url: String): Boolean =
        url.contains("account_contactlist.php", ignoreCase = true)
}

/** Desktop [net.sourceforge.kolmafia.request.PasswordHashRequest] pwd scrape. */
object PasswordHashRequest {
    private val PWD = Regex("""(?:pwd|hash)=([0-9a-fA-F]{10,})""")

    fun parseResponse(url: String, html: String, preferences: net.sourceforge.kolmafia.preferences.Preferences?) {
        val fromUrl = PWD.find(url)?.groupValues?.getOrNull(1)
        val fromHtml = Regex("""name=["']pwd["'][^>]*value=["']([^"']+)["']""").find(html)
            ?.groupValues?.getOrNull(1)
        val hash = fromHtml ?: fromUrl ?: return
        preferences?.setString("pwdHash", hash)
    }

    fun registerRequest(url: String): Boolean = hashFrom(url) != null

    private fun hashFrom(url: String): String? = PWD.find(url)?.groupValues?.getOrNull(1)
}

/** Desktop [net.sourceforge.kolmafia.request.MoonPhaseRequest]. */
object MoonPhaseRequest {
    fun parseResponse(html: String, preferences: net.sourceforge.kolmafia.preferences.Preferences?) {
        val ronald = Regex("""Ronald.*?phase (\d+)""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val grimace = Regex("""Grimace.*?phase (\d+)""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (ronald != null) preferences?.setInt("moonRonaldPhase", ronald)
        if (grimace != null) preferences?.setInt("moonGrimacePhase", grimace)
    }

    fun registerRequest(url: String): Boolean = url.contains("mountains.php", ignoreCase = true)
}

/** Desktop [net.sourceforge.kolmafia.request.ChannelColorsRequest] chat colors. */
object ChannelColorsRequest {
    fun parseResponse(url: String, html: String, preferences: net.sourceforge.kolmafia.preferences.Preferences?) {
        if (!url.contains("account_chatcolors.php", ignoreCase = true) &&
            !url.contains("chatcolors", ignoreCase = true)
        ) {
            return
        }
        val colors = Regex("""channel[_-]?color[^>]*>([^<]+)""")
            .findAll(html)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotBlank() }
            .toList()
        if (colors.isNotEmpty()) {
            preferences?.setString("chatChannelColors", colors.joinToString("|"))
        }
    }

    fun registerRequest(url: String): Boolean =
        url.contains("chatcolor", ignoreCase = true)
}
