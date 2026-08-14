package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.MainStat
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop GongCommand — set path prefs + use llama lama gong (no buy / finishLlamaForm). */
class GongRequest(
    private val useItemRequest: UseItemRequest,
) {
    suspend fun run(
        parameters: String,
        preferences: Preferences?,
        charState: CharacterState?,
        inventoryCounts: (Int) -> Int,
        activeEffects: List<EffectData> = emptyList(),
    ): Result<String> {
        val prefs = preferences
            ?: return Result.failure(IllegalStateException("Preferences are not available."))
        val parse = parseParameters(parameters, charState?.mainStat ?: MainStat.MUSCLE)
            .getOrElse { return Result.failure(it) }
        setPath(parse.path, prefs)
        if (parse.setOnly) {
            return Result.success("Gong path set: ${GONG_PATHS.getOrElse(parse.path) { parse.path.toString() }}")
        }
        val blocked = preflightBlocked(charState, activeEffects)
        if (blocked != null) {
            return Result.failure(IllegalStateException(blocked))
        }
        if (inventoryCounts(ItemDatabase.GONG) <= 0) {
            return Result.failure(
                IllegalStateException("You don't have a llama lama gong."),
            )
        }
        return useItemRequest.use(ItemDatabase.GONG, 1)
    }

    companion object {
        val GONG_PATHS = arrayOf(
            "show in browser",
            "bird",
            "mole",
            "roach (in browser)",
            "musc, musc, +30% musc",
            "musc, mox, +30% musc",
            "musc, MP, +30% musc",
            "myst, musc, +30% myst",
            "myst, myst, +30% myst",
            "myst, MP, +30% myst",
            "mox, myst, +30% mox",
            "mox, mox, +30% mox",
            "mox, MP, +30% mox",
            "musc, musc, +10% all",
            "myst, musc, +10% all",
            "musc, mox, +10% all",
            "myst, myst, +10% all",
            "mox, mox, +10% all",
            "mox, MP, +10% all",
            "musc, musc, +50% items",
            "myst, musc, +50% items",
            "musc, MP, +50% items",
            "mox, myst, +50% items",
            "myst, MP, +50% items",
            "mox, MP, +50% items",
            "musc, mox, +30 ML",
            "musc, MP, +30 ML",
            "myst, myst, +30 ML",
            "mox, myst, +30 ML",
            "myst, MP, +30 ML",
            "mox, mox, +30 ML",
        )

        private val GONG_CHOICES = intArrayOf(
            0x00000004,
            0x00000007,
            0x00000006,
            0x00000005,
            0x00004095,
            0x00001055,
            0x000100d5,
            0x00300225,
            0x00040125,
            0x00800325,
            0x08000835,
            0x02000435,
            0x10000c35,
            0x00008095,
            0x00200225,
            0x00002055,
            0x000c0125,
            0x03000435,
            0x20000c35,
            0x0000c095,
            0x00100225,
            0x000200d5,
            0x0c000835,
            0x00c00325,
            0x30000c35,
            0x00003055,
            0x000300d5,
            0x00080125,
            0x04000835,
            0x00400325,
            0x01000435,
        )

        private val GONG_SEARCH = intArrayOf(
            0x3336a8e4,
            0x374728e4,
            0x3367ace5,
            0x35593126,
            0x334728e4,
            0x37482904,
            0x3967a8e5,
            0x3b793126,
            0x3337ace5,
            0x396728e5,
            0x3d68ace5,
            0x35893126,
            0x3556b0e6,
            0x3b772926,
            0x33893125,
            0x35593126,
            0x3336a8e4,
            0x374728e4,
            0x3367a8e5,
            0x35593126,
            0x334728e4,
            0x37482904,
            0x3968a905,
            0x3b793126,
            0x3347a8e5,
            0x39682905,
            0x3d68ad05,
            0x3b893126,
            0x355730e6,
            0x3b782926,
            0x39893125,
            0x3b793126,
            0x3336ace4,
            0x394728e5,
            0x3367ace5,
            0x35593126,
            0x334728e5,
            0x37682905,
            0x3968a905,
            0x3b793126,
            0x3337ace5,
            0x39682905,
            0x3d68ace5,
            0x35893126,
            0x3557b0e6,
            0x3b782926,
            0x3d893125,
            0x35893126,
        )

        data class ParsedGong(
            val path: Int,
            val setOnly: Boolean,
            val buy: Boolean = false,
        )

        fun parseParameters(
            parameters: String,
            mainStat: MainStat = MainStat.MUSCLE,
        ): Result<ParsedGong> {
            val parts = parameters.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            var pos = 0
            var buy = false
            var set = false
            if (pos < parts.size && parts[pos].equals("buy", ignoreCase = true)) {
                buy = true
                pos++
            } else if (pos < parts.size && parts[pos].equals("set", ignoreCase = true)) {
                set = true
                pos++
            }
            if (pos >= parts.size) {
                return Result.failure(
                    IllegalArgumentException(
                        "Usage: gong [buy | set] manual | bird | mole | roach [<effect> [<stat> [<stat>]]]",
                    ),
                )
            }
            if (buy) {
                return Result.failure(
                    IllegalArgumentException("Gong buy is not supported yet."),
                )
            }
            var path = parseOption(parts, pos++, "manual bird mole roach")
                .getOrElse { return Result.failure(it) }
            if (path == 3 && pos < parts.size) {
                val effect = parseOption(
                    parts,
                    pos++,
                    "mus ack mys alc mox rad all new item ext ml unp",
                ).getOrElse { return Result.failure(it) } / 2
                var primary = primeIndex(mainStat)
                var secondary = primary
                val main = primary
                if (pos < parts.size) {
                    primary = parseOption(parts, pos++, "mus mys mox mp")
                        .getOrElse { return Result.failure(it) }
                }
                if (pos < parts.size) {
                    secondary = parseOption(parts, pos++, "mus mys mox mp")
                        .getOrElse { return Result.failure(it) }
                }
                val packed = GONG_SEARCH[primary + 4 * secondary + 16 * main]
                path = packed shr (5 * effect) and 0x1F
            }
            if (pos < parts.size) {
                return Result.failure(
                    IllegalArgumentException(
                        "Unexpected text after command, starting with: ${parts[pos]}",
                    ),
                )
            }
            return Result.success(ParsedGong(path = path, setOnly = set, buy = buy))
        }

        fun setPath(path: Int, preferences: Preferences) {
            if (path < 0 || path >= GONG_PATHS.size) return
            preferences.setInt("gongPath", path)
            var bits = GONG_CHOICES[path]
            for (i in 276..290) {
                preferences.setString("choiceAdventure$i", (bits and 0x03).toString())
                bits = bits shr 2
            }
        }

        fun preflightBlocked(
            charState: CharacterState?,
            activeEffects: List<EffectData>,
        ): String? {
            val limitMode = charState?.limitMode.orEmpty()
            val limited = when (limitMode.lowercase()) {
                // Desktop finishLlamaForm adventuring is out of scope this phase —
                // refuse while still in bird/mole limit mode.
                "bird", "mole" -> true
                "roach" -> hasEffectId(activeEffects, FORM_OF_ROACH)
                else -> LimitModeGates.limitItem(limitMode, ItemDatabase.GONG)
            }
            return if (limited) "You can't use a gong right now." else null
        }

        fun primeIndex(stat: MainStat): Int = when (stat) {
            MainStat.MUSCLE -> 0
            MainStat.MYSTICALITY -> 1
            MainStat.MOXIE -> 2
        }

        const val FORM_OF_ROACH = 509

        private fun hasEffectId(effects: List<EffectData>, id: Int): Boolean =
            effects.any { it.id == id }

        private fun parseOption(
            parts: List<String>,
            pos: Int,
            optionString: String,
        ): Result<Int> {
            if (pos >= parts.size) {
                return Result.failure(IllegalArgumentException("Expected one of: $optionString"))
            }
            val options = optionString.split(" ")
            val param = parts[pos].lowercase()
            options.forEachIndexed { i, option ->
                if (param.startsWith(option)) return Result.success(i)
            }
            return Result.failure(
                IllegalArgumentException("Found '$param', but expected one of: $optionString"),
            )
        }
    }
}
