package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Desktop [ConsequenceManager.descriptions] — ordered desc URLs from consequences.txt. */
@OptIn(ExperimentalResourceApi::class)
object DescriptionConsequenceRegistry {

    private val urls = mutableListOf<String>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/consequences.txt").decodeToString()
        urls.clear()
        urls.addAll(parse(text))
        loaded = true
    }

    fun all(): List<String> = urls.toList()

    fun urlForDay(dayDifference: Long): String? {
        if (urls.isEmpty()) return null
        val index = ((dayDifference % urls.size) + urls.size) % urls.size
        return urls[index.toInt()]
    }

    internal fun parseForTest(text: String): List<String> = parse(text)

    internal fun injectForTest(catalog: List<String>) {
        urls.clear()
        urls.addAll(catalog)
        loaded = true
    }

    internal fun resetForTest() {
        urls.clear()
        loaded = false
    }

    private fun parse(text: String): List<String> {
        val result = mutableListOf<String>()
        for (raw in text.lines()) {
            val line = raw.trimEnd('\r', '\n')
            if (line.trim().isEmpty() || line.trimStart().startsWith("#")) continue
            if (!line.contains('\t')) continue
            val parts = splitPreservingTrailingEmpty(line)
            if (parts.size < 4) continue
            when (parts[0]) {
                "DESC_ITEM" -> {
                    val item = ItemDatabase.getByName(parts[1]) ?: continue
                    if (item.descId.isNotEmpty()) {
                        result.add("desc_item.php?whichitem=${item.descId}")
                    }
                }
                "DESC_EFFECT" -> {
                    val effect = EffectDatabase.getByName(parts[1]) ?: continue
                    if (effect.descId.isNotEmpty()) {
                        result.add("desc_effect.php?whicheffect=${effect.descId}")
                    }
                }
                "DESC_SKILL" -> {
                    val skill = SkillDefinitionDatabase.getByName(parts[1]) ?: continue
                    result.add("desc_skill.php?whichskill=${skill.id}&self=true")
                }
            }
        }
        return result
    }

    /** Desktop [FileUtilities.readData] uses `split("\t", -1)`. */
    internal fun splitPreservingTrailingEmpty(line: String, delimiter: Char = '\t'): List<String> {
        val result = mutableListOf<String>()
        var start = 0
        for (i in line.indices) {
            if (line[i] == delimiter) {
                result.add(line.substring(start, i))
                start = i + 1
            }
        }
        result.add(line.substring(start))
        return result
    }
}
