package net.sourceforge.kolmafia.data



import net.sourceforge.kolmafia.shared.generated.resources.Res

import org.jetbrains.compose.resources.ExperimentalResourceApi



// Parses statuseffects.txt from bundled compose resources.

// Format (tab-separated): id  name  image  descid  quality  attributes  default_action

@OptIn(ExperimentalResourceApi::class)

object EffectDatabase {



    private val byId = mutableMapOf<Int, EffectData>()

    private val byName = mutableMapOf<String, EffectData>()

    private val byDescId = mutableMapOf<String, EffectData>()

    private val bundledById = mutableMapOf<Int, EffectData>()

    private var loaded = false



    suspend fun load() {

        if (loaded) return

        val text = Res.readBytes("files/data/statuseffects.txt").decodeToString()

        parse(text)

        snapshotBundled()

        loaded = true

    }



    fun getById(id: Int): EffectData? = byId[id]

    fun getByName(name: String): EffectData? = byName[name.lowercase()]

    fun getByDescId(descId: String): EffectData? = byDescId[descId]

    fun getByIdOrName(effectRef: String): EffectData? = EffectDefinitionProxy.getByIdOrName(effectRef)

    fun resolveEffectId(effectRef: String): Int = EffectDefinitionProxy.resolveEffectId(effectRef)

    fun all(): Collection<EffectData> = byId.values



    /** Desktop EffectDatabase — runtime TCRS action override. */

    fun updateActions(effectId: Int, actions: String?) {

        val existing = byId[effectId] ?: return

        putEffect(existing.copy(actions = actions))

    }



    /** Remove eat/drink/chew/use clauses from all effect actions (TCRS apply preamble). */

    fun stripConsumableActions() {

        for ((id, effect) in byId.toList()) {

            val actions = effect.actions ?: continue

            if (actions.startsWith("#")) continue

            if (!actions.contains("eat ") &&

                !actions.contains("drink ") &&

                !actions.contains("chew ") &&

                !actions.contains("use ")

            ) {

                continue

            }

            val kept = actions.split(Regex(" *\\| *"))

                .filter { clause ->

                    clause.isNotEmpty() &&

                        !clause.startsWith("eat ") &&

                        !clause.startsWith("drink ") &&

                        !clause.startsWith("chew ") &&

                        !clause.startsWith("use ")

                }

            updateActions(id, kept.joinToString("|").ifEmpty { null })

        }

    }



    /** Desktop TCRSDatabase.addEffectSource — patch effect actions with a TCRS item source. */

    fun addEffectSource(itemName: String, primaryUse: ItemPrimaryUse?, effectName: String) {

        val effectId = getByName(effectName)?.id ?: return

        val effect = byId[effectId] ?: return

        val verb = when (primaryUse) {

            ItemPrimaryUse.FOOD -> "eat "

            ItemPrimaryUse.DRINK -> "drink "

            ItemPrimaryUse.SPLEEN -> "chew "

            else -> "use "

        }

        val actions = effect.actions

        var added = false

        val buffer = StringBuilder()

        if (actions != null) {

            val either = verb + "either "

            for (action in actions.split(Regex(" *\\| *"))) {

                if (action.isEmpty()) continue

                if (buffer.isNotEmpty()) buffer.append("|")

                if (added) {

                    buffer.append(action)

                    continue

                }

                when {

                    action.startsWith(either) -> {

                        buffer.append(action)

                        buffer.append(", 1 ")

                    }

                    action.startsWith(verb) -> {

                        buffer.append(action.replaceFirst(verb, either))

                        buffer.append(", 1 ")

                    }

                    else -> {

                        buffer.append(action)

                        continue

                    }

                }

                buffer.append(itemName)

                added = true

            }

        }

        if (!added) {

            if (buffer.isNotEmpty()) buffer.append("|")

            buffer.append(verb)

            buffer.append("1 ")

            buffer.append(itemName)

        }

        updateActions(effectId, buffer.toString())

    }



    /** Restore runtime action overrides from bundled statuseffects.txt snapshot. */

    fun resetOverrides() {

        byId.clear()

        byName.clear()

        byDescId.clear()

        for (effect in bundledById.values) {

            putEffect(effect)

        }

    }



    /** Test hook — register an effect without loading statuseffects.txt. */

    internal fun registerForTest(effect: EffectData) {

        putEffect(effect)

        bundledById[effect.id] = effect

        loaded = true

    }



    internal fun resetForTest() {

        byId.clear()

        byName.clear()

        byDescId.clear()

        bundledById.clear()

        loaded = false

    }



    fun goodEffects(): List<EffectData> = byId.values.filter { it.quality == EffectQuality.GOOD }

    fun badEffects(): List<EffectData> = byId.values.filter { it.quality == EffectQuality.BAD }



    private fun putEffect(effect: EffectData) {

        byId[effect.id] = effect

        byName[effect.name.lowercase()] = effect

        byDescId[effect.descId] = effect

    }



    private fun snapshotBundled() {

        bundledById.clear()

        for (effect in byId.values) {

            bundledById[effect.id] = effect

        }

    }



    private fun parse(text: String) {

        for (raw in text.lines()) {

            val line = raw.trim()

            if (line.isEmpty() || line.startsWith("#")) continue

            val firstToken = line.substringBefore('\t')

            if (firstToken.toIntOrNull() != null && line.count { it == '\t' } < 3) continue



            val parts = line.split('\t')

            if (parts.size < 6) continue



            val id = parts[0].toIntOrNull() ?: continue

            val name = parts[1]

            val image = parts[2]

            val descId = parts[3]

            val quality = when (parts[4].trim().lowercase()) {

                "good" -> EffectQuality.GOOD

                "bad" -> EffectQuality.BAD

                "neutral" -> EffectQuality.NEUTRAL

                else -> EffectQuality.UNKNOWN

            }

            val attrStr = parts[5].trim()

            val attributes = if (attrStr == "none") emptySet()

                             else attrStr.split(',').map { it.trim() }.toSet()

            val actions = parts.getOrNull(6)?.trim()?.takeIf { it.isNotEmpty() }



            putEffect(EffectData(id, name, image, descId, quality, attributes, actions))

        }

    }

}


