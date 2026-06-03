package net.sourceforge.kolmafia.ash

open class AshValue internal constructor(open val type: AshType, val content: Any?) {
    init {
        when (type) {
            AshType.INT -> require(content is Long?) { "INT value must be Long, got ${content?.let { it::class.simpleName }}" }
            AshType.FLOAT -> require(content is Double?) { "FLOAT value must be Double, got ${content?.let { it::class.simpleName }}" }
            AshType.BOOLEAN -> require(content is Boolean?) { "BOOLEAN value must be Boolean, got ${content?.let { it::class.simpleName }}" }
            else -> {} // other types accept Any?
        }
    }

    override fun equals(other: Any?): Boolean =
        other is AshValue && type == other.type && content == other.content

    override fun hashCode(): Int = 31 * type.hashCode() + (content?.hashCode() ?: 0)

    override fun toString(): String = when {
        type == AshType.VOID -> "void"
        type == AshType.BOOLEAN -> if (content as Boolean) "true" else "false"
        type == AshType.INT -> (content as Long).toString()
        type == AshType.FLOAT -> (content as Double).toString()
        type == AshType.STRING -> content as String
        type == AshType.BUFFER -> (content as StringBuilder).toString()
        else -> content?.toString() ?: ""
    }

    fun toBoolean(): Boolean = when {
        type == AshType.BOOLEAN -> content as Boolean
        type == AshType.INT -> (content as Long) != 0L
        type == AshType.FLOAT -> (content as Double) != 0.0
        type == AshType.STRING -> (content as String).isNotEmpty()
        else -> false
    }

    fun toLong(): Long = when {
        type == AshType.INT -> content as Long
        type == AshType.FLOAT -> (content as Double).toLong()
        type == AshType.BOOLEAN -> if (content as Boolean) 1L else 0L
        type == AshType.STRING -> (content as String).toLongOrNull() ?: 0L
        else -> 0L
    }

    fun toDouble(): Double = when {
        type == AshType.FLOAT -> content as Double
        type == AshType.INT -> (content as Long).toDouble()
        type == AshType.BOOLEAN -> if (content as Boolean) 1.0 else 0.0
        type == AshType.STRING -> (content as String).toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    fun coerceTo(target: AshType): AshValue = when {
        type == target -> this
        target == AshType.STRING -> of(toString())
        target == AshType.FLOAT -> of(toDouble())
        target == AshType.INT -> of(toLong())
        target == AshType.BOOLEAN -> of(toBoolean())
        else -> throw ScriptException("Cannot coerce ${type.name} to ${target.name}")
    }

    companion object {
        val VOID = AshValue(AshType.VOID, null)
        val TRUE = AshValue(AshType.BOOLEAN, true)
        val FALSE = AshValue(AshType.BOOLEAN, false)
        val ZERO = AshValue(AshType.INT, 0L)
        val ONE = AshValue(AshType.INT, 1L)
        val EMPTY_STRING = AshValue(AshType.STRING, "")

        fun of(v: Boolean): AshValue = if (v) TRUE else FALSE
        fun of(v: Long): AshValue = AshValue(AshType.INT, v)
        fun of(v: Int): AshValue = AshValue(AshType.INT, v.toLong())
        fun of(v: Double): AshValue = AshValue(AshType.FLOAT, v)
        fun of(v: String): AshValue = AshValue(AshType.STRING, v)

        fun item(name: String): AshValue = AshValue(AshType.ITEM, name)
        fun location(name: String): AshValue = AshValue(AshType.LOCATION, name)
        fun skill(name: String): AshValue = AshValue(AshType.SKILL, name)
        fun effect(name: String): AshValue = AshValue(AshType.EFFECT, name)
        fun familiar(name: String): AshValue = AshValue(AshType.FAMILIAR, name)
    }
}

class AggregateValue(override val type: AggregateType) : AshValue(type, null) {
    val map: LinkedHashMap<AshValue, AshValue> = LinkedHashMap()

    operator fun get(key: AshValue): AshValue = map[key] ?: type.dataType.defaultValue()
    operator fun set(key: AshValue, value: AshValue) { map[key] = value }

    fun size(): AshValue = AshValue.of(map.size)

    fun keys(): AggregateValue {
        val result = AggregateValue(AggregateType(AshType.INT, type.indexType))
        map.keys.forEachIndexed { i, k -> result[AshValue.of(i)] = k }
        return result
    }

    override fun toString() = map.entries.joinToString(", ", "{", "}") { (k, v) -> "$k => $v" }
}

class RecordValue(override val type: RecordType) : AshValue(type, null) {
    private val fields: Array<AshValue?> = arrayOfNulls(type.fields.size)

    fun getField(index: Int): AshValue = fields[index] ?: type.fields[index].type.defaultValue()
    fun setField(index: Int, value: AshValue) { fields[index] = value }

    fun getField(name: String): AshValue {
        val f = type.fields.find { it.name.equals(name, ignoreCase = true) }
            ?: throw ScriptException("Record '${type.name}' has no field '$name'")
        return getField(f.index)
    }

    fun setField(name: String, value: AshValue) {
        val f = type.fields.find { it.name.equals(name, ignoreCase = true) }
            ?: throw ScriptException("Record '${type.name}' has no field '$name'")
        setField(f.index, value)
    }

    override fun toString() = type.fields.joinToString(", ", "{", "}") { f ->
        "${f.name}: ${getField(f.index)}"
    }
}
