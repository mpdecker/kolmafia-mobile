package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.StringModifier

/** Desktop [ModifierList] subset used by TCRS derive (order-preserving, quoted string values). */
class TCRSModifierList {
    data class ModifierValue(val name: String, val value: String?)

    private val list = ArrayList<ModifierValue>()

    fun addModifier(name: String, value: String?) {
        list += ModifierValue(name, value)
    }

    fun addModifier(other: ModifierValue) {
        list += other
    }

    fun addToModifier(name: String, value: String?) {
        val current = getModifierValue(name)
        if (current == null) {
            addModifier(name, value)
            return
        }
        val curNum = current.toIntOrNull()
        val addNum = value?.toIntOrNull()
        if (curNum != null && addNum != null) {
            removeModifier(name)
            addModifier(name, (curNum + addNum).toString())
        }
    }

    fun containsModifier(name: String): Boolean = list.any { it.name == name }

    fun getModifierValue(name: String): String? = list.firstOrNull { it.name == name }?.value

    fun removeModifier(name: String): ModifierValue? {
        val idx = list.indexOfFirst { it.name == name }
        if (idx < 0) return null
        return list.removeAt(idx)
    }

    fun iterator(): Iterator<ModifierValue> = list.iterator()

    override fun toString(): String =
        list.joinToString(", ") { mv ->
            if (mv.value == null) mv.name
            else if (StringModifier.byTag(mv.name) != null && !mv.value.startsWith("\"")) {
                "${mv.name}: \"${mv.value}\""
            } else {
                "${mv.name}: ${mv.value}"
            }
        }

    companion object {
        fun split(modifiers: String?): TCRSModifierList {
            val list = TCRSModifierList()
            var rest = modifiers
            while (rest != null) {
                var comma = rest.indexOf(',')
                if (comma != -1) {
                    val bracket1 = rest.indexOf('[')
                    if (bracket1 != -1 && bracket1 < comma) {
                        val bracket2 = rest.indexOf(']', bracket1 + 1)
                        comma = if (bracket2 != -1) rest.indexOf(',', bracket2 + 1) else -1
                    } else {
                        val quote1 = rest.indexOf('"')
                        if (quote1 != -1 && quote1 < comma) {
                            val quote2 = rest.indexOf('"', quote1 + 1)
                            comma = if (quote2 != -1) rest.indexOf(',', quote2 + 1) else -1
                        }
                    }
                }
                val string: String
                if (comma == -1) {
                    string = rest
                    rest = null
                } else {
                    string = rest.substring(0, comma).trim()
                    rest = rest.substring(comma + 1).trim()
                }
                val colon = string.indexOf(": ")
                val key: String
                val value: String?
                if (colon == -1) {
                    key = string
                    value = null
                } else {
                    key = string.substring(0, colon)
                    value = string.substring(colon + 2)
                }
                if (!(key.isEmpty() && value == null)) {
                    list.addModifier(key, value)
                }
            }
            return list
        }

        fun isEnchantment(name: String): Boolean =
            DoubleModifier.byTag(name)?.isEnchantment() == true ||
                StringModifier.byTag(name)?.isEnchantment() == true ||
                BooleanModifier.byTag(name)?.isEnchantment() == true

        fun appendModifier(known: TCRSModifierList, mod: String?) {
            if (mod == null) return
            if (mod.contains('"') || !mod.contains(',')) {
                val mv = makeModifier(mod)
                known.addToModifier(mv.name, mv.value)
                return
            }
            for (s in mod.split(',')) {
                val mv = makeModifier(s)
                known.addToModifier(mv.name, mv.value)
            }
        }

        private fun makeModifier(mod: String): ModifierValue {
            val colon = mod.indexOf(':')
            val key = if (colon == -1) mod.trim() else mod.substring(0, colon).trim()
            val value = if (colon == -1) null else mod.substring(colon + 1).trim()
            return ModifierValue(key, value)
        }
    }
}
