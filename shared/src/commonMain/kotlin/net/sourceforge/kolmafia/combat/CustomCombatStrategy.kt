package net.sourceforge.kolmafia.combat

/**
 * Desktop [CustomCombatStrategy] — one CCS section's action list with section inlining.
 */
class CustomCombatStrategy(val name: String) {
    private val children = mutableListOf<CustomCombatAction>()
    private var actionCount: Int = 0
    private var actionOffsets: IntArray? = null

    fun getChildCount(): Int = children.size

    fun getChildAt(index: Int): CustomCombatAction = children[index]

    fun getLastChild(): CustomCombatAction? = children.lastOrNull()

    fun removeAllChildren() {
        resetActionCount()
        children.clear()
    }

    fun resetActionCount() {
        actionCount = 0
        actionOffsets = null
    }

    fun getActionCount(lookup: CustomCombatLookup, seen: MutableSet<String> = mutableSetOf()): Int {
        if (name in seen) return 0
        seen.add(name)
        actionOffsets?.let { return actionCount }

        val childCount = children.size
        actionCount = 0
        val offsets = IntArray(childCount)
        for (i in 0 until childCount) {
            offsets[i] = actionCount
            val actionNode = children[i]
            val sectionReference = actionNode.sectionReference
            val strategy = sectionReference?.let { lookup.getStrategy(it) }
            when {
                strategy != null -> actionCount += strategy.getActionCount(lookup, seen)
                sectionReference != null -> Unit // invalid section — desktop aborts; we skip expand
                else -> actionCount++
            }
        }
        actionOffsets = offsets
        return actionCount
    }

    fun getAction(lookup: CustomCombatLookup, roundIndex: Int, allowMacro: Boolean): String {
        if (children.isEmpty()) return "attack"
        getActionCount(lookup)

        val childCount = children.size
        val offsets = actionOffsets ?: return "attack"

        for (i in 0 until childCount) {
            if (offsets[i] > roundIndex) {
                val actionNode = children[i - 1]
                val sectionReference = actionNode.sectionReference
                if (sectionReference != null) {
                    val offset = if (i > 0) offsets[i - 1] else 0
                    val strategy = lookup.getStrategy(sectionReference)
                    return strategy?.getAction(lookup, roundIndex - offset, allowMacro) ?: "abort"
                }
                if (!allowMacro && actionNode.isMacro) return "skip"
                return actionNode.action
            }
        }

        val actionNode = children.last()
        val sectionReference = actionNode.sectionReference
        if (sectionReference != null) {
            val strategy = lookup.getStrategy(sectionReference)
            return strategy?.getAction(lookup, roundIndex - offsets[childCount - 1], allowMacro)
                ?: "abort"
        }
        if (!allowMacro && actionNode.isMacro) return "skip"
        return actionNode.action
    }

    fun addCombatAction(roundIndex: Int, indent: String, combatAction: String, isMacro: Boolean) {
        val currentIndex = children.size
        if (roundIndex <= currentIndex) return
        addRepeatActions(roundIndex, indent)
        children.add(CustomCombatAction(roundIndex, indent, combatAction, isMacro))
        resetActionCount()
    }

    private fun addRepeatActions(roundIndex: Int, indent: String) {
        var currentIndex = children.size
        if (roundIndex <= currentIndex) return
        var repeatAction = "attack with weapon"
        var isMacro = false
        if (currentIndex > 0) {
            val node = children.last()
            repeatAction = node.action
            isMacro = node.isMacro
        }
        for (i in (currentIndex + 1) until roundIndex) {
            children.add(CustomCombatAction(i, indent, repeatAction, isMacro))
        }
    }

    fun store(): String = buildString {
        appendLine("[ $name ]")
        for (action in children) {
            appendLine(action.storeLine())
        }
        appendLine()
    }
}
