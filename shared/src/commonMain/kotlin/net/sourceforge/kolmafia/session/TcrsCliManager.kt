package net.sourceforge.kolmafia.session

import io.ktor.client.HttpClient
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.TCRSDatabase
import net.sourceforge.kolmafia.data.TCRSDeriver
import net.sourceforge.kolmafia.data.TCRSRemoteFetch
import net.sourceforge.kolmafia.preferences.Preferences

/** Validated local TCRS operations exposed by the `tcrs` CLI command. */
open class TcrsCliManager(
    private val character: KoLCharacter? = null,
    private val preferences: Preferences? = null,
    private val httpClient: HttpClient? = null,
) {
    private fun identity(): Pair<String, String>? {
        val state = character?.state?.value ?: return null
        val className = state.className
        val sign = state.zodiacSign
        return (className to sign).takeIf { TCRSDatabase.validate(className, sign) }
    }

    fun status(): List<String> {
        val state = character?.state?.value
        val identity = identity()
        return listOf(
            "TCRS path: ${state?.inTwoCrazyRandomSummer == true}",
            "Class: ${state?.className.orEmpty().ifBlank { "(unknown)" }}",
            "Sign: ${state?.zodiacSign.orEmpty().ifBlank { "(unknown)" }}",
            "Loaded data: ${if (identity != null && TCRSDatabase.isLoaded()) TCRSDatabase.entryCount().toString() else "no"} entries",
        )
    }

    fun load(): String {
        val (className, sign) = identity() ?: return "Current class/sign is not valid for TCRS."
        val loaded = preferences?.let { TCRSDatabase.loadFromPreferences(className, sign, it) } == true
        return if (loaded) "Loaded TCRS data for $className, $sign."
        else "No saved TCRS data for $className, $sign."
    }

    fun save(): String {
        val (className, sign) = identity() ?: return "Current class/sign is not valid for TCRS."
        val saved = preferences?.let { TCRSDatabase.saveToPreferences(className, sign, it) } == true
        return if (saved) "Saved TCRS data for $className, $sign."
        else "Unable to save TCRS data."
    }

    suspend fun fetch(): List<String> {
        val (className, sign) = identity()
            ?: return listOf("Current class/sign is not valid for TCRS.")
        val client = httpClient
            ?: return listOf("HTTP client is unavailable for TCRS fetch.")
        val lines = mutableListOf<String>()
        var anyLoaded = false
        for (suffix in listOf("", "_cafe_booze", "_cafe_food")) {
            val filename = TCRSDatabase.filename(className, sign, suffix)
            if (filename.isBlank()) continue
            when (val result = TCRSRemoteFetch.fetchText(client, filename)) {
                is TCRSRemoteFetch.FetchResult.Success -> {
                    val count = TCRSDatabase.importFetchedText(className, sign, suffix, result.text)
                    lines += "Fetched $filename ($count entries)."
                    anyLoaded = anyLoaded || count > 0
                }
                is TCRSRemoteFetch.FetchResult.AlreadyFetched -> {
                    lines += "Already fetched remote version of $filename in this session."
                }
                is TCRSRemoteFetch.FetchResult.Empty -> {
                    lines += "File $filename is empty."
                }
                is TCRSRemoteFetch.FetchResult.Failed -> {
                    lines += "Failed to fetch $filename: ${result.reason}"
                }
            }
        }
        if (anyLoaded) {
            preferences?.let { TCRSDatabase.saveToPreferences(className, sign, it) }
            lines += "Saved fetched TCRS data for $className, $sign."
        }
        return lines
    }

    fun apply(): String {
        val state = character?.state?.value ?: return "Character state is unavailable."
        if (identity() == null) return "Current class/sign is not valid for TCRS."
        val count = TCRSDatabase.applyModifiers(state.level)
        return "Applied TCRS modifiers ($count entries)."
    }

    fun reset(): String {
        val state = character?.state?.value ?: return "Character state is unavailable."
        TCRSDatabase.resetModifiers(preferences ?: return "Preferences are unavailable.", state.level)
        return "Reset TCRS modifiers."
    }

    fun derive(itemId: Int?): String {
        if (identity() == null) return "Current class/sign is not valid for TCRS."
        if (itemId != null) {
            val cached = TCRSDeriver.deriveFromCache(itemId)
            if (cached != null) {
                TCRSDatabase.putDerivedEntry(itemId, cached)
                return "Derived item #$itemId: ${formatEntry(itemId, cached)}"
            }
            return "No cached description for item #$itemId; visit desc_item first or use fetched TCRS data."
        }
        return if (TCRSDatabase.isLoaded()) {
            "Derived ${TCRSDatabase.entryCount()} loaded TCRS entries; use `tcrs check <item id>` to inspect one."
        } else {
            "No TCRS data loaded; use `tcrs fetch` or `tcrs load` first."
        }
    }

    fun check(itemId: Int): String {
        if (identity() == null) return "Current class/sign is not valid for TCRS."
        val entry = TCRSDatabase.getEntry(itemId)
            ?: return "No loaded TCRS entry for item #$itemId."
        return formatEntry(itemId, entry)
    }

    private fun formatEntry(itemId: Int, entry: TCRSDatabase.TcrsEntry): String =
        "Item #$itemId: name=${entry.name}; size=${entry.size}; quality=${entry.quality}; modifiers='${entry.modifiers}'"
}
