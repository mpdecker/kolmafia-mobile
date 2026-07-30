package net.sourceforge.kolmafia.data

/** Desktop EffectDatabase helpers for `$effect[field]` proxy reads. */
object EffectDefinitionProxy {

    fun getByIdOrName(effectRef: String): EffectData? {
        effectRef.toIntOrNull()?.let { EffectDatabase.getById(it) }?.let { return it }
        return EffectDatabase.getByName(effectRef)
    }

    fun resolveEffectId(effectRef: String): Int =
        effectRef.toIntOrNull()
            ?: EffectDatabase.getByName(effectRef)?.id
            ?: 0

    fun getActions(effectId: Int): String? {
        if (effectId == -1) return null
        return EffectDatabase.getById(effectId)?.actions
    }

    fun getDefaultAction(effectId: Int): String? {
        if (effectId == -1) return null
        val actions = getActions(effectId) ?: return null
        if (actions.startsWith("#")) return null
        val pieces = actions.split('|')
        return pieces.firstOrNull()
    }

    fun getActionNote(effectId: Int): String? {
        if (effectId == -1) return null
        val actions = getActions(effectId) ?: return null
        if (actions.startsWith("#")) {
            return actions.substring(1).trim()
        }
        return null
    }

    fun getAllActions(effectId: Int): List<String> {
        if (effectId == -1) return emptyList()
        val actions = getActions(effectId) ?: return emptyList()
        val result = mutableListOf<String>()
        for (piece in actions.split('|')) {
            val either = piece.split(' ', limit = 3)
            if (either.size == 3 && either[1] == "either") {
                val cmd = either[0]
                for (target in either[2].split(',')) {
                    result.add("$cmd ${target.trim()}")
                }
            } else {
                result.add(piece)
            }
        }
        return result
    }
}
