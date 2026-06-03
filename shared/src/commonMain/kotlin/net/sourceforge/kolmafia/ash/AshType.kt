package net.sourceforge.kolmafia.ash

open class AshType(val name: String) {
    override fun equals(other: Any?) = other is AshType && name == other.name && this::class == other::class
    override fun hashCode() = name.hashCode() * 31 + this::class.hashCode()
    override fun toString() = name

    open val isAggregate: Boolean = false
    open val isRecord: Boolean = false

    fun defaultValue(): AshValue = when {
        this == VOID -> AshValue.VOID
        this == BOOLEAN -> AshValue.FALSE
        this == INT -> AshValue.ZERO
        this == FLOAT -> AshValue.of(0.0)
        this == STRING -> AshValue.EMPTY_STRING
        this == BUFFER -> AshValue(BUFFER, StringBuilder())
        isAggregate -> AggregateValue(this as AggregateType)
        isRecord -> RecordValue(this as RecordType)
        else -> AshValue(this, "") // game entity types: empty string = "none"
    }

    companion object {
        val VOID = AshType("void")
        val BOOLEAN = AshType("boolean")
        val INT = AshType("int")
        val FLOAT = AshType("float")
        val STRING = AshType("string")
        val BUFFER = AshType("buffer")
        val ITEM = AshType("item")
        val LOCATION = AshType("location")
        val CLASS = AshType("class")
        val STAT = AshType("stat")
        val SKILL = AshType("skill")
        val EFFECT = AshType("effect")
        val FAMILIAR = AshType("familiar")
        val SLOT = AshType("slot")
        val MONSTER = AshType("monster")
        val ELEMENT = AshType("element")
        val COINMASTER = AshType("coinmaster")
        val PHYLUM = AshType("phylum")
        val PATH = AshType("path")

        private val PRIMITIVES: Map<String, AshType> = mapOf(
            "void" to VOID, "boolean" to BOOLEAN, "int" to INT, "float" to FLOAT,
            "string" to STRING, "buffer" to BUFFER, "item" to ITEM,
            "location" to LOCATION, "class" to CLASS, "stat" to STAT,
            "skill" to SKILL, "effect" to EFFECT, "familiar" to FAMILIAR,
            "slot" to SLOT, "monster" to MONSTER, "element" to ELEMENT,
            "coinmaster" to COINMASTER, "phylum" to PHYLUM, "path" to PATH
        )

        fun fromName(name: String, records: Map<String, RecordType> = emptyMap()): AshType? =
            PRIMITIVES[name.lowercase()] ?: records[name.lowercase()]

        fun canCoerce(from: AshType, to: AshType): Boolean = when {
            from == to -> true
            from == INT && to == FLOAT -> true
            from == BOOLEAN && to == INT -> true
            to == STRING -> true
            from is AggregateType && to is AggregateType && from.dataType == to.dataType -> true
            else -> false
        }
    }
}

class AggregateType(
    val indexType: AshType,
    val dataType: AshType,
    val fixedSize: Int = -1
) : AshType(
    if (fixedSize >= 0) "${dataType.name}[$fixedSize]"
    else "${dataType.name}[${indexType.name}]"
) {
    override val isAggregate = true
    override fun equals(other: Any?) = other is AggregateType &&
        indexType == other.indexType && dataType == other.dataType && fixedSize == other.fixedSize
    override fun hashCode() = 31 * (31 * indexType.hashCode() + dataType.hashCode()) + fixedSize
}

class RecordType(name: String, val fields: List<RecordField>) : AshType(name) {
    override val isRecord = true
    override fun equals(other: Any?) = other is RecordType && name == other.name
    override fun hashCode() = name.hashCode()
}

data class RecordField(val name: String, val type: AshType, val index: Int)
