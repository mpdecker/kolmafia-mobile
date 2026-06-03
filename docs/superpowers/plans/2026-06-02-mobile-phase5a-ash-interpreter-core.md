# KoLmafia Mobile — Phase 5a: ASH Interpreter Core

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the KoLmafia ASH (Adventure Script Handler) interpreter to Kotlin Multiplatform `commonMain` — type system, parse tree nodes, recursive-descent parser, and execution engine — producing a fully tested interpreter that can run ASH scripts in unit tests with no device or game connection required.

**Architecture:** Three layers in `shared/commonMain/kotlin/net/sourceforge/kolmafia/ash/`: (1) `AshType` / `AshValue` — the type and value system; (2) `ParseTreeNode` sealed hierarchy + `AshScope` + `AshOperator` — the abstract syntax tree model; (3) `AshParser` — tokenizer + recursive descent parser producing a `ParseTreeNode` list; (4) `AshRuntime` — tree-walking execution engine. A minimal `RuntimeLibrary` stub provides `print()` and `to_string()` so runtime tests can verify output without the full Phase 5b library. Phase 5b layers game-facing built-ins and UI on top.

**Tech Stack:** Existing stack — kotlin.test, kotlinx.coroutines-test, MockEngine. No new dependencies.

**Source reference:** Java original at `src/net/sourceforge/kolmafia/textui/` in the kolmafia repo. Key files: `Parser.java` (5,749 lines), `AshRuntime.java` (535 lines), `DataTypes.java` (1,126 lines), 68 parsetree node classes.

---

## File Map

```
shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/
  AshType.kt          ← open class + AggregateType + RecordType + RecordField; defaultValue()
  AshValue.kt         ← open class + AggregateValue + RecordValue; coerceTo(), toBoolean/Long/Double
  ParseTreeNode.kt    ← sealed class: all statement + expression AST nodes + LvalueNode
  AshScope.kt         ← AshVariable, AshFunction, AshScope (chain with parent)
  AshOperator.kt      ← enum: all operators, apply(left, right), applyUnary(operand)
  ScriptException.kt  ← RuntimeException with optional line number
  AshParser.kt        ← TokenType enum, Token data class, tokenize(), recursive descent parse()
  AshRuntime.kt       ← execute(List<ParseTreeNode>): AshValue; evalExpr(); executeNode()
  RuntimeLibrary.kt   ← stub: registers print(), to_string() only (full set in Phase 5b)

shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/
  AshTypeTest.kt      ← defaultValue(), canCoerce(), AggregateType/RecordType construction
  AshParserTest.kt    ← literals, binary expressions, all statement types, function defs, records
  AshRuntimeTest.kt   ← expression evaluation, control flow, user functions, foreach, try/catch
```

---

## Task 1: AshType + AshValue

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/AshType.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/AshValue.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ScriptException.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/AshTypeTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/AshTypeTest.kt
package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AshTypeTest {

    @Test
    fun fromName_returnsCorrectPrimitive() {
        assertEquals(AshType.INT, AshType.fromName("int"))
        assertEquals(AshType.STRING, AshType.fromName("string"))
        assertEquals(AshType.BOOLEAN, AshType.fromName("boolean"))
        assertEquals(AshType.FLOAT, AshType.fromName("float"))
        assertEquals(AshType.VOID, AshType.fromName("void"))
        assertEquals(AshType.ITEM, AshType.fromName("item"))
        assertEquals(AshType.SKILL, AshType.fromName("skill"))
        assertEquals(AshType.EFFECT, AshType.fromName("effect"))
        assertEquals(AshType.FAMILIAR, AshType.fromName("familiar"))
        assertEquals(AshType.LOCATION, AshType.fromName("location"))
    }

    @Test
    fun fromName_caseInsensitive() {
        assertEquals(AshType.INT, AshType.fromName("INT"))
        assertEquals(AshType.STRING, AshType.fromName("String"))
    }

    @Test
    fun fromName_unknownReturnsNull() {
        assertNull(AshType.fromName("notatype"))
    }

    @Test
    fun canCoerce_sameType() {
        assertTrue(AshType.canCoerce(AshType.INT, AshType.INT))
        assertTrue(AshType.canCoerce(AshType.STRING, AshType.STRING))
    }

    @Test
    fun canCoerce_intToFloat() {
        assertTrue(AshType.canCoerce(AshType.INT, AshType.FLOAT))
        assertFalse(AshType.canCoerce(AshType.FLOAT, AshType.INT))
    }

    @Test
    fun canCoerce_anythingToString() {
        assertTrue(AshType.canCoerce(AshType.INT, AshType.STRING))
        assertTrue(AshType.canCoerce(AshType.BOOLEAN, AshType.STRING))
        assertTrue(AshType.canCoerce(AshType.FLOAT, AshType.STRING))
    }

    @Test
    fun aggregateType_name() {
        val t = AggregateType(AshType.STRING, AshType.INT)
        assertEquals("int[string]", t.name)
    }

    @Test
    fun aggregateType_fixedSize_name() {
        val t = AggregateType(AshType.INT, AshType.BOOLEAN, fixedSize = 5)
        assertEquals("boolean[5]", t.name)
    }

    @Test
    fun recordType_construction() {
        val fields = listOf(RecordField("hp", AshType.INT, 0), RecordField("name", AshType.STRING, 1))
        val t = RecordType("PlayerData", fields)
        assertEquals("PlayerData", t.name)
        assertEquals(2, t.fields.size)
        assertTrue(t.isRecord)
    }

    @Test
    fun defaultValue_int_isZero() {
        val v = AshType.INT.defaultValue()
        assertEquals(0L, v.toLong())
    }

    @Test
    fun defaultValue_boolean_isFalse() {
        assertFalse(AshType.BOOLEAN.defaultValue().toBoolean())
    }

    @Test
    fun defaultValue_string_isEmpty() {
        assertEquals("", AshType.STRING.defaultValue().toString())
    }

    @Test
    fun defaultValue_aggregate_isEmpty() {
        val aggType = AggregateType(AshType.STRING, AshType.INT)
        val v = aggType.defaultValue() as AggregateValue
        assertEquals(0, v.map.size)
    }

    @Test
    fun ashValue_coerceTo_intToFloat() {
        val v = AshValue.of(3)
        val f = v.coerceTo(AshType.FLOAT)
        assertEquals(3.0, f.toDouble())
    }

    @Test
    fun ashValue_coerceTo_anyToString() {
        assertEquals("42", AshValue.of(42).coerceTo(AshType.STRING).toString())
        assertEquals("true", AshValue.of(true).coerceTo(AshType.STRING).toString())
    }

    @Test
    fun ashValue_toBoolean_variants() {
        assertTrue(AshValue.of(1).toBoolean())
        assertFalse(AshValue.of(0).toBoolean())
        assertTrue(AshValue.of("hello").toBoolean())
        assertFalse(AshValue.of("").toBoolean())
        assertTrue(AshValue.of(true).toBoolean())
        assertFalse(AshValue.of(false).toBoolean())
    }

    @Test
    fun aggregateValue_getSet() {
        val t = AggregateType(AshType.STRING, AshType.INT)
        val v = AggregateValue(t)
        v[AshValue.of("foo")] = AshValue.of(42)
        assertEquals(42L, v[AshValue.of("foo")].toLong())
        assertEquals(0L, v[AshValue.of("bar")].toLong()) // default
    }

    @Test
    fun recordValue_getSetField() {
        val fields = listOf(RecordField("hp", AshType.INT, 0), RecordField("name", AshType.STRING, 1))
        val t = RecordType("PC", fields)
        val v = RecordValue(t)
        v.setField(0, AshValue.of(100))
        v.setField(1, AshValue.of("Testington"))
        assertEquals(100L, v.getField(0).toLong())
        assertEquals("Testington", v.getField(1).toString())
    }
}
```

- [ ] **Step 2: Run — verify it fails**

```bash
cd C:\Development\kolmafia-mobile
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.ash.AshTypeTest"
```

Expected: FAIL — `AshType` does not exist yet.

- [ ] **Step 3: Write `ScriptException.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ScriptException.kt
package net.sourceforge.kolmafia.ash

class ScriptException(message: String, val line: Int = -1) : Exception(
    if (line > 0) "Script error at line $line: $message" else "Script error: $message"
)
```

- [ ] **Step 4: Write `AshType.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/AshType.kt
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
```

- [ ] **Step 5: Write `AshValue.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/AshValue.kt
package net.sourceforge.kolmafia.ash

open class AshValue(val type: AshType, val content: Any?) {

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

        // Game entity constructors
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
```

- [ ] **Step 6: Run — verify tests pass**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.ash.AshTypeTest"
```

Expected: PASS (18 tests)

- [ ] **Step 7: Commit**

```bash
git add shared/src/
git commit -m "feat: ASH type system — AshType, AshValue, AggregateValue, RecordValue, ScriptException"
```

---

## Task 2: ParseTreeNode + AshScope + AshFunction

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ParseTreeNode.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/AshScope.kt`

- [ ] **Step 1: Write `ParseTreeNode.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ParseTreeNode.kt
package net.sourceforge.kolmafia.ash

sealed class ParseTreeNode

// --- Statements ---

data class IfNode(
    val condition: ExprNode,
    val thenBlock: List<ParseTreeNode>,
    val elseIfClauses: List<Pair<ExprNode, List<ParseTreeNode>>>,
    val elseBlock: List<ParseTreeNode>?
) : ParseTreeNode()

data class WhileNode(val condition: ExprNode, val body: List<ParseTreeNode>) : ParseTreeNode()

data class RepeatNode(val body: List<ParseTreeNode>, val condition: ExprNode) : ParseTreeNode()

data class ForNode(
    val varName: String,
    val start: ExprNode,
    val end: ExprNode,
    val step: ExprNode?,
    val ascending: Boolean   // true = to/upto, false = downto
) : ParseTreeNode()

data class ForEachNode(
    val keyNames: List<String>,
    val aggregate: ExprNode,
    val body: List<ParseTreeNode>
) : ParseTreeNode()

data class TryNode(
    val body: List<ParseTreeNode>,
    val catchBlock: List<ParseTreeNode>?
) : ParseTreeNode()

data class ReturnNode(val value: ExprNode?) : ParseTreeNode()
object BreakNode : ParseTreeNode()
object ContinueNode : ParseTreeNode()
object ExitNode : ParseTreeNode()

data class VarDeclNode(
    val varName: String,
    val type: AshType,
    val init: ExprNode?
) : ParseTreeNode()

data class FunctionDefNode(val function: AshFunction) : ParseTreeNode()
data class RecordDefNode(val recordType: RecordType) : ParseTreeNode()

data class ExprStatement(val expr: ExprNode) : ParseTreeNode()

// --- Expressions ---

sealed class ExprNode : ParseTreeNode()

data class LiteralNode(val value: AshValue) : ExprNode()
data class VarRefNode(val name: String) : ExprNode()
data class AssignNode(val target: LvalueNode, val value: ExprNode) : ExprNode()
data class BinOpNode(val op: AshOperator, val left: ExprNode, val right: ExprNode) : ExprNode()
data class UnaryOpNode(val op: AshOperator, val operand: ExprNode) : ExprNode()
data class TernaryNode(val condition: ExprNode, val thenExpr: ExprNode, val elseExpr: ExprNode) : ExprNode()
data class FunctionCallNode(val name: String, val args: List<ExprNode>) : ExprNode()
data class IndexNode(val aggregate: ExprNode, val index: ExprNode) : ExprNode()
data class FieldAccessNode(val record: ExprNode, val fieldName: String) : ExprNode()
data class AggregateLiteralNode(
    val aggType: AggregateType,
    val entries: List<Pair<ExprNode?, ExprNode>>
) : ExprNode()
data class RecordLiteralNode(
    val recordType: RecordType,
    val fieldExprs: List<ExprNode?>
) : ExprNode()

// --- Lvalues (assignment targets) ---

sealed class LvalueNode : ParseTreeNode()
data class VarLvalue(val name: String) : LvalueNode()
data class IndexLvalue(val aggregate: ExprNode, val index: ExprNode) : LvalueNode()
data class FieldLvalue(val record: ExprNode, val fieldName: String) : LvalueNode()
```

- [ ] **Step 2: Write `AshScope.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/AshScope.kt
package net.sourceforge.kolmafia.ash

data class AshVariable(val name: String, val type: AshType, var value: AshValue)

data class AshFunction(
    val name: String,
    val returnType: AshType,
    val params: List<Pair<String, AshType>>,
    val body: List<ParseTreeNode>?,
    val libraryImpl: ((AshRuntime, List<AshValue>) -> AshValue)? = null
)

class AshScope(val parent: AshScope? = null) {
    private val variables: MutableMap<String, AshVariable> = mutableMapOf()
    private val functions: MutableMap<String, MutableList<AshFunction>> = mutableMapOf()

    fun declareVar(name: String, type: AshType, value: AshValue? = null) {
        variables[name.lowercase()] = AshVariable(name, type, value ?: type.defaultValue())
    }

    fun getVar(name: String): AshVariable? =
        variables[name.lowercase()] ?: parent?.getVar(name)

    fun setVar(name: String, value: AshValue) {
        val key = name.lowercase()
        if (variables.containsKey(key)) { variables[key]!!.value = value; return }
        parent?.setVar(name, value) ?: throw ScriptException("Undefined variable: $name")
    }

    fun declareFunction(fn: AshFunction) {
        functions.getOrPut(fn.name.lowercase()) { mutableListOf() }.add(fn)
    }

    fun resolveFunction(name: String, argTypes: List<AshType>): AshFunction? {
        val candidates = functions[name.lowercase()]
        if (candidates != null) {
            val match = candidates.find { fn ->
                fn.params.size == argTypes.size &&
                fn.params.zip(argTypes).all { (param, arg) -> AshType.canCoerce(arg, param.second) }
            }
            if (match != null) return match
        }
        return parent?.resolveFunction(name, argTypes)
    }

    fun child(): AshScope = AshScope(this)
}
```

- [ ] **Step 3: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add shared/src/
git commit -m "feat: ParseTreeNode sealed hierarchy, AshScope, AshVariable, AshFunction"
```

---

## Task 3: AshOperator

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/AshOperator.kt`

- [ ] **Step 1: Write `AshOperator.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/AshOperator.kt
package net.sourceforge.kolmafia.ash

import kotlin.math.pow

enum class AshOperator(val symbol: String) {
    ADD("+"), SUB("-"), MUL("*"), DIV("/"), REM("%"), POW("**"),
    EQ("=="), NEQ("!="), LT("<"), LE("<="), GT(">"), GE(">="),
    AND("&&"), OR("||"), NOT("!"),
    BAND("&"), BOR("|"), BXOR("^"), BNOT("~"),
    NEGATE("-"),
    PRE_INC("++"), PRE_DEC("--"), POST_INC("++"), POST_DEC("--");

    fun apply(left: AshValue, right: AshValue): AshValue {
        val bothNumeric = (left.type == AshType.INT || left.type == AshType.FLOAT) &&
                          (right.type == AshType.INT || right.type == AshType.FLOAT)
        val useFloat = bothNumeric &&
                       (left.type == AshType.FLOAT || right.type == AshType.FLOAT)
        return when (this) {
            ADD -> when {
                left.type == AshType.STRING || right.type == AshType.STRING ->
                    AshValue.of(left.toString() + right.toString())
                useFloat -> AshValue.of(left.toDouble() + right.toDouble())
                else -> AshValue.of(left.toLong() + right.toLong())
            }
            SUB -> if (useFloat) AshValue.of(left.toDouble() - right.toDouble())
                   else AshValue.of(left.toLong() - right.toLong())
            MUL -> if (useFloat) AshValue.of(left.toDouble() * right.toDouble())
                   else AshValue.of(left.toLong() * right.toLong())
            DIV -> if (useFloat) {
                if (right.toDouble() == 0.0) throw ScriptException("Division by zero")
                AshValue.of(left.toDouble() / right.toDouble())
            } else {
                if (right.toLong() == 0L) throw ScriptException("Division by zero")
                AshValue.of(left.toLong() / right.toLong())
            }
            REM -> {
                if (right.toLong() == 0L) throw ScriptException("Modulo by zero")
                AshValue.of(left.toLong() % right.toLong())
            }
            POW -> AshValue.of(left.toDouble().pow(right.toDouble()))
            EQ -> AshValue.of(left.toString() == right.toString())
            NEQ -> AshValue.of(left.toString() != right.toString())
            LT -> numericCompare(left, right) { a, b -> a < b }
            LE -> numericCompare(left, right) { a, b -> a <= b }
            GT -> numericCompare(left, right) { a, b -> a > b }
            GE -> numericCompare(left, right) { a, b -> a >= b }
            AND -> AshValue.of(left.toBoolean() && right.toBoolean())
            OR -> AshValue.of(left.toBoolean() || right.toBoolean())
            BAND -> AshValue.of(left.toLong() and right.toLong())
            BOR -> AshValue.of(left.toLong() or right.toLong())
            BXOR -> AshValue.of(left.toLong() xor right.toLong())
            else -> throw ScriptException("Operator $symbol is not binary")
        }
    }

    fun applyUnary(operand: AshValue): AshValue = when (this) {
        NOT -> AshValue.of(!operand.toBoolean())
        NEGATE -> when (operand.type) {
            AshType.FLOAT -> AshValue.of(-operand.toDouble())
            else -> AshValue.of(-operand.toLong())
        }
        BNOT -> AshValue.of(operand.toLong().inv())
        PRE_INC, POST_INC -> when (operand.type) {
            AshType.FLOAT -> AshValue.of(operand.toDouble() + 1.0)
            else -> AshValue.of(operand.toLong() + 1L)
        }
        PRE_DEC, POST_DEC -> when (operand.type) {
            AshType.FLOAT -> AshValue.of(operand.toDouble() - 1.0)
            else -> AshValue.of(operand.toLong() - 1L)
        }
        else -> throw ScriptException("Operator $symbol is not unary")
    }

    private fun numericCompare(
        left: AshValue, right: AshValue, cmp: (Double, Double) -> Boolean
    ): AshValue = AshValue.of(cmp(left.toDouble(), right.toDouble()))
}
```

- [ ] **Step 2: Verify compile**

```bash
./gradlew :shared:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add shared/src/
git commit -m "feat: AshOperator enum with apply() and applyUnary()"
```

---

## Task 4: AshParser — Lexer

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/AshParser.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/AshParserTest.kt`

- [ ] **Step 1: Write the failing tests for tokenization and literal parsing**

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/AshParserTest.kt
package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AshParserTest {

    private fun parse(src: String) = AshParser().parse(src)
    private fun parseExpr(src: String): ExprNode =
        (parse("$src;")[0] as ExprStatement).expr

    // --- Literals ---

    @Test
    fun literal_int() {
        val lit = parseExpr("42") as LiteralNode
        assertEquals(42L, lit.value.toLong())
    }

    @Test
    fun literal_float() {
        val lit = parseExpr("3.14") as LiteralNode
        assertEquals(3.14, lit.value.toDouble(), 0.001)
    }

    @Test
    fun literal_true() {
        val lit = parseExpr("true") as LiteralNode
        assertTrue(lit.value.toBoolean())
    }

    @Test
    fun literal_false() {
        val lit = parseExpr("false") as LiteralNode
        assertEquals(false, lit.value.toBoolean())
    }

    @Test
    fun literal_string() {
        val lit = parseExpr("\"hello\"") as LiteralNode
        assertEquals("hello", lit.value.toString())
    }

    @Test
    fun literal_string_escapes() {
        val lit = parseExpr("\"a\\nb\"") as LiteralNode
        assertEquals("a\nb", lit.value.toString())
    }

    // --- Binary expressions and precedence ---

    @Test
    fun binaryExpr_add() {
        val node = parseExpr("1 + 2") as BinOpNode
        assertEquals(AshOperator.ADD, node.op)
    }

    @Test
    fun precedence_mulOverAdd() {
        val node = parseExpr("1 + 2 * 3") as BinOpNode
        assertEquals(AshOperator.ADD, node.op)
        assertIs<BinOpNode>(node.right).also {
            assertEquals(AshOperator.MUL, it.op)
        }
    }

    @Test
    fun precedence_powOverMul() {
        val node = parseExpr("2 * 3 ** 4") as BinOpNode
        assertEquals(AshOperator.MUL, node.op)
        assertIs<BinOpNode>(node.right).also {
            assertEquals(AshOperator.POW, it.op)
        }
    }

    @Test
    fun precedence_andOverOr() {
        val node = parseExpr("a || b && c") as BinOpNode
        assertEquals(AshOperator.OR, node.op)
        assertIs<BinOpNode>(node.right).also {
            assertEquals(AshOperator.AND, it.op)
        }
    }

    @Test
    fun unaryNot() {
        val node = parseExpr("!true") as UnaryOpNode
        assertEquals(AshOperator.NOT, node.op)
    }

    @Test
    fun unaryNegate() {
        val node = parseExpr("-5") as UnaryOpNode
        assertEquals(AshOperator.NEGATE, node.op)
    }

    @Test
    fun ternary() {
        val node = parseExpr("true ? 1 : 2") as TernaryNode
        assertIs<LiteralNode>(node.condition)
        assertIs<LiteralNode>(node.thenExpr)
        assertIs<LiteralNode>(node.elseExpr)
    }

    // --- Statements ---

    @Test
    fun varDecl_withInit() {
        val node = parse("int x = 5;")[0] as VarDeclNode
        assertEquals("x", node.varName)
        assertEquals(AshType.INT, node.type)
        assertIs<LiteralNode>(node.init)
    }

    @Test
    fun varDecl_withoutInit() {
        val node = parse("string s;")[0] as VarDeclNode
        assertEquals("s", node.varName)
        assertNull(node.init)
    }

    @Test
    fun ifStatement_thenOnly() {
        val node = parse("if (true) { }")[0] as IfNode
        assertTrue(node.elseBlock == null)
        assertTrue(node.elseIfClauses.isEmpty())
    }

    @Test
    fun ifStatement_withElse() {
        val node = parse("if (x) { } else { }")[0] as IfNode
        assertTrue(node.elseBlock != null)
    }

    @Test
    fun ifStatement_elseIf() {
        val node = parse("if (a) { } else if (b) { } else { }")[0] as IfNode
        assertEquals(1, node.elseIfClauses.size)
        assertTrue(node.elseBlock != null)
    }

    @Test
    fun whileStatement() {
        val node = parse("while (true) { }")[0] as WhileNode
        assertIs<LiteralNode>(node.condition)
    }

    @Test
    fun repeatStatement() {
        val node = parse("repeat { } until (false);")[0] as RepeatNode
        assertIs<LiteralNode>(node.condition)
    }

    @Test
    fun forStatement() {
        val node = parse("for i from 1 to 10 { }")[0] as ForNode
        assertEquals("i", node.varName)
        assertTrue(node.ascending)
    }

    @Test
    fun forStatement_downto() {
        val node = parse("for i from 10 downto 1 { }")[0] as ForNode
        assertEquals(false, node.ascending)
    }

    @Test
    fun foreachStatement() {
        val node = parse("foreach k in myMap { }")[0] as ForEachNode
        assertEquals(listOf("k"), node.keyNames)
    }

    @Test
    fun foreachStatement_twoKeys() {
        val node = parse("foreach k, v in myMap { }")[0] as ForEachNode
        assertEquals(listOf("k", "v"), node.keyNames)
    }

    @Test
    fun tryStatement_withCatch() {
        val node = parse("try { } catch { }")[0] as TryNode
        assertTrue(node.catchBlock != null)
    }

    @Test
    fun returnStatement_withValue() {
        val fn = parse("int f() { return 42; }")[0] as FunctionDefNode
        val ret = fn.function.body!![0] as ReturnNode
        assertIs<LiteralNode>(ret.value)
    }

    @Test
    fun breakStatement() {
        val fn = parse("void f() { break; }")[0] as FunctionDefNode
        assertIs<BreakNode>(fn.function.body!![0])
    }

    @Test
    fun functionDef_params() {
        val node = parse("int add(int a, int b) { return a + b; }")[0] as FunctionDefNode
        val fn = node.function
        assertEquals("add", fn.name)
        assertEquals(AshType.INT, fn.returnType)
        assertEquals(2, fn.params.size)
        assertEquals("a" to AshType.INT, fn.params[0])
        assertEquals("b" to AshType.INT, fn.params[1])
    }

    @Test
    fun recordDef() {
        val node = parse("record Point { int x; int y; }")[0] as RecordDefNode
        assertEquals("Point", node.recordType.name)
        assertEquals(2, node.recordType.fields.size)
        assertEquals("x", node.recordType.fields[0].name)
        assertEquals(AshType.INT, node.recordType.fields[0].type)
    }

    @Test
    fun indexExpr() {
        val node = parseExpr("arr[0]") as IndexNode
        assertIs<VarRefNode>(node.aggregate)
        assertIs<LiteralNode>(node.index)
    }

    @Test
    fun fieldAccess() {
        val node = parseExpr("rec.hp") as FieldAccessNode
        assertEquals("rec", (node.record as VarRefNode).name)
        assertEquals("hp", node.fieldName)
    }

    @Test
    fun assignment_simple() {
        val node = parseExpr("x = 5") as AssignNode
        assertIs<VarLvalue>(node.target)
    }

    @Test
    fun assignment_indexed() {
        val node = parseExpr("arr[0] = 5") as AssignNode
        assertIs<IndexLvalue>(node.target)
    }

    @Test
    fun compoundAssignment_plusEq() {
        val node = parseExpr("x += 1") as AssignNode
        assertIs<VarLvalue>(node.target)
        val rhs = node.value as BinOpNode
        assertEquals(AshOperator.ADD, rhs.op)
    }

    @Test
    fun functionCall_noArgs() {
        val node = parseExpr("foo()") as FunctionCallNode
        assertEquals("foo", node.name)
        assertTrue(node.args.isEmpty())
    }

    @Test
    fun functionCall_withArgs() {
        val node = parseExpr("bar(1, 2)") as FunctionCallNode
        assertEquals(2, node.args.size)
    }

    @Test
    fun preIncrement() {
        val node = parseExpr("++x") as UnaryOpNode
        assertEquals(AshOperator.PRE_INC, node.op)
    }

    @Test
    fun postIncrement() {
        val node = parseExpr("x++") as UnaryOpNode
        assertEquals(AshOperator.POST_INC, node.op)
    }

    @Test
    fun lineComment_skipped() {
        val nodes = parse("// this is a comment\nint x = 1;")
        assertEquals(1, nodes.size)
    }

    @Test
    fun blockComment_skipped() {
        val nodes = parse("/* block */ int x = 1;")
        assertEquals(1, nodes.size)
    }

    @Test
    fun hashComment_skipped() {
        val nodes = parse("# hash comment\nint x = 1;")
        assertEquals(1, nodes.size)
    }
}
```

- [ ] **Step 2: Run — verify it fails**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.ash.AshParserTest"
```

Expected: FAIL — `AshParser` does not exist yet.

- [ ] **Step 3: Write `AshParser.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/AshParser.kt
package net.sourceforge.kolmafia.ash

enum class TokenType {
    INT_LIT, FLOAT_LIT, BOOL_LIT, STRING_LIT,
    IDENT,
    IF, ELSE, WHILE, REPEAT, UNTIL,
    FOR, FROM, TO, UPTO, DOWNTO, BY,
    FOREACH, IN, TRY, CATCH,
    RETURN, BREAK, CONTINUE, EXIT, RECORD,
    PLUS, MINUS, STAR, SLASH, PERCENT, STAR_STAR,
    EQ_EQ, BANG_EQ, LT, LE, GT, GE,
    AMP_AMP, PIPE_PIPE, BANG,
    AMP, PIPE, CARET, TILDE,
    PLUS_PLUS, MINUS_MINUS,
    PLUS_EQ, MINUS_EQ, STAR_EQ, SLASH_EQ, PERCENT_EQ,
    EQ, QUESTION, COLON,
    LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET,
    SEMICOLON, COMMA, DOT,
    EOF
}

data class Token(val type: TokenType, val text: String, val line: Int)

class AshParser {
    private var tokens: List<Token> = emptyList()
    private var pos: Int = 0
    private val knownRecords: MutableMap<String, RecordType> = mutableMapOf()

    private companion object {
        val KEYWORDS: Map<String, TokenType> = mapOf(
            "if" to TokenType.IF, "else" to TokenType.ELSE,
            "while" to TokenType.WHILE, "repeat" to TokenType.REPEAT, "until" to TokenType.UNTIL,
            "for" to TokenType.FOR, "from" to TokenType.FROM, "to" to TokenType.TO,
            "upto" to TokenType.UPTO, "downto" to TokenType.DOWNTO, "by" to TokenType.BY,
            "foreach" to TokenType.FOREACH, "in" to TokenType.IN,
            "try" to TokenType.TRY, "catch" to TokenType.CATCH,
            "return" to TokenType.RETURN, "break" to TokenType.BREAK,
            "continue" to TokenType.CONTINUE, "exit" to TokenType.EXIT,
            "record" to TokenType.RECORD
        )
    }

    // ──────────────────────────────────────────────────────────────
    // Lexer
    // ──────────────────────────────────────────────────────────────

    private fun tokenize(source: String): List<Token> {
        val result = mutableListOf<Token>()
        var i = 0
        var line = 1

        while (i < source.length) {
            while (i < source.length && source[i].isWhitespace()) {
                if (source[i] == '\n') line++
                i++
            }
            if (i >= source.length) break
            val c = source[i]

            // line comments: # or //
            if (c == '#' || (c == '/' && i + 1 < source.length && source[i + 1] == '/')) {
                while (i < source.length && source[i] != '\n') i++
                continue
            }
            // block comment: /* ... */
            if (c == '/' && i + 1 < source.length && source[i + 1] == '*') {
                i += 2
                while (i + 1 < source.length && !(source[i] == '*' && source[i + 1] == '/')) {
                    if (source[i] == '\n') line++
                    i++
                }
                i += 2
                continue
            }
            // string literal
            if (c == '"') {
                val sb = StringBuilder(); i++
                while (i < source.length && source[i] != '"') {
                    if (source[i] == '\\' && i + 1 < source.length) {
                        i++
                        sb.append(when (source[i]) {
                            'n' -> '\n'; 't' -> '\t'; 'r' -> '\r'; '"' -> '"'; '\\' -> '\\'
                            else -> source[i]
                        })
                    } else sb.append(source[i])
                    i++
                }
                i++
                result.add(Token(TokenType.STRING_LIT, sb.toString(), line))
                continue
            }
            // number literal
            if (c.isDigit() || (c == '.' && i + 1 < source.length && source[i + 1].isDigit())) {
                val start = i; var isFloat = false
                while (i < source.length && source[i].isDigit()) i++
                if (i < source.length && source[i] == '.') { isFloat = true; i++; while (i < source.length && source[i].isDigit()) i++ }
                result.add(Token(if (isFloat) TokenType.FLOAT_LIT else TokenType.INT_LIT, source.substring(start, i), line))
                continue
            }
            // identifier or keyword
            if (c.isLetter() || c == '_') {
                val start = i
                while (i < source.length && (source[i].isLetterOrDigit() || source[i] == '_')) i++
                val text = source.substring(start, i)
                when (text.lowercase()) {
                    "true", "false" -> result.add(Token(TokenType.BOOL_LIT, text, line))
                    else -> result.add(Token(KEYWORDS[text.lowercase()] ?: TokenType.IDENT, text, line))
                }
                continue
            }
            // operators and punctuation
            fun ch(offset: Int = 1): Char? = if (i + offset < source.length) source[i + offset] else null
            val tok = when {
                c == '+' && ch() == '+' -> Token(TokenType.PLUS_PLUS, "++", line).also { i += 2 }
                c == '+' && ch() == '=' -> Token(TokenType.PLUS_EQ, "+=", line).also { i += 2 }
                c == '+' -> Token(TokenType.PLUS, "+", line).also { i++ }
                c == '-' && ch() == '-' -> Token(TokenType.MINUS_MINUS, "--", line).also { i += 2 }
                c == '-' && ch() == '=' -> Token(TokenType.MINUS_EQ, "-=", line).also { i += 2 }
                c == '-' -> Token(TokenType.MINUS, "-", line).also { i++ }
                c == '*' && ch() == '*' -> Token(TokenType.STAR_STAR, "**", line).also { i += 2 }
                c == '*' && ch() == '=' -> Token(TokenType.STAR_EQ, "*=", line).also { i += 2 }
                c == '*' -> Token(TokenType.STAR, "*", line).also { i++ }
                c == '/' && ch() == '=' -> Token(TokenType.SLASH_EQ, "/=", line).also { i += 2 }
                c == '/' -> Token(TokenType.SLASH, "/", line).also { i++ }
                c == '%' && ch() == '=' -> Token(TokenType.PERCENT_EQ, "%=", line).also { i += 2 }
                c == '%' -> Token(TokenType.PERCENT, "%", line).also { i++ }
                c == '=' && ch() == '=' -> Token(TokenType.EQ_EQ, "==", line).also { i += 2 }
                c == '=' -> Token(TokenType.EQ, "=", line).also { i++ }
                c == '!' && ch() == '=' -> Token(TokenType.BANG_EQ, "!=", line).also { i += 2 }
                c == '!' -> Token(TokenType.BANG, "!", line).also { i++ }
                c == '<' && ch() == '=' -> Token(TokenType.LE, "<=", line).also { i += 2 }
                c == '<' -> Token(TokenType.LT, "<", line).also { i++ }
                c == '>' && ch() == '=' -> Token(TokenType.GE, ">=", line).also { i += 2 }
                c == '>' -> Token(TokenType.GT, ">", line).also { i++ }
                c == '&' && ch() == '&' -> Token(TokenType.AMP_AMP, "&&", line).also { i += 2 }
                c == '&' -> Token(TokenType.AMP, "&", line).also { i++ }
                c == '|' && ch() == '|' -> Token(TokenType.PIPE_PIPE, "||", line).also { i += 2 }
                c == '|' -> Token(TokenType.PIPE, "|", line).also { i++ }
                c == '^' -> Token(TokenType.CARET, "^", line).also { i++ }
                c == '~' -> Token(TokenType.TILDE, "~", line).also { i++ }
                c == '?' -> Token(TokenType.QUESTION, "?", line).also { i++ }
                c == ':' -> Token(TokenType.COLON, ":", line).also { i++ }
                c == '(' -> Token(TokenType.LPAREN, "(", line).also { i++ }
                c == ')' -> Token(TokenType.RPAREN, ")", line).also { i++ }
                c == '{' -> Token(TokenType.LBRACE, "{", line).also { i++ }
                c == '}' -> Token(TokenType.RBRACE, "}", line).also { i++ }
                c == '[' -> Token(TokenType.LBRACKET, "[", line).also { i++ }
                c == ']' -> Token(TokenType.RBRACKET, "]", line).also { i++ }
                c == ';' -> Token(TokenType.SEMICOLON, ";", line).also { i++ }
                c == ',' -> Token(TokenType.COMMA, ",", line).also { i++ }
                c == '.' -> Token(TokenType.DOT, ".", line).also { i++ }
                else -> throw ScriptException("Unexpected character '$c'", line)
            }
            result.add(tok)
        }
        result.add(Token(TokenType.EOF, "", line))
        return result
    }

    // ──────────────────────────────────────────────────────────────
    // Token utilities
    // ──────────────────────────────────────────────────────────────

    private fun current(): Token = tokens[pos]
    private fun check(type: TokenType): Boolean = current().type == type
    private fun checkIdent(text: String): Boolean = check(TokenType.IDENT) && current().text.equals(text, ignoreCase = true)
    private fun checkNext(type: TokenType): Boolean = pos + 1 < tokens.size && tokens[pos + 1].type == type
    private fun advance(): Token = tokens[pos++]

    private fun expect(type: TokenType): Token {
        if (!check(type)) throw ScriptException("Expected $type but got ${current().type} ('${current().text}')", current().line)
        return advance()
    }

    private fun expectIdent(): String {
        if (current().type != TokenType.IDENT) throw ScriptException("Expected identifier, got '${current().text}'", current().line)
        return advance().text
    }

    private fun currentLine(): Int = current().line

    // ──────────────────────────────────────────────────────────────
    // Type parsing
    // ──────────────────────────────────────────────────────────────

    private fun isTypeName(text: String): Boolean =
        AshType.fromName(text, knownRecords) != null

    private fun parseTypeToken(): AshType {
        val name = current().text
        if (!isTypeName(name)) throw ScriptException("Unknown type '$name'", currentLine())
        advance()
        if (!check(TokenType.LBRACKET)) return AshType.fromName(name, knownRecords)!!
        advance()
        return if (tokens[pos].type == TokenType.INT_LIT) {
            val size = tokens[pos].text.toInt(); advance()
            expect(TokenType.RBRACKET)
            val base = AshType.fromName(name, knownRecords)!!
            AggregateType(AshType.INT, base, size)
        } else {
            val indexName = current().text; advance()
            expect(TokenType.RBRACKET)
            val base = AshType.fromName(name, knownRecords)!!
            val indexType = AshType.fromName(indexName, knownRecords)
                ?: throw ScriptException("Unknown index type '$indexName'", currentLine())
            AggregateType(indexType, base)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Expression parsing (precedence climbing)
    // ──────────────────────────────────────────────────────────────

    private fun parseExpr(): ExprNode {
        val left = parseTernary()
        return when {
            check(TokenType.EQ) -> { advance(); AssignNode(toTarget(left), parseTernary()) }
            check(TokenType.PLUS_EQ) -> { advance(); val t = toTarget(left); AssignNode(t, BinOpNode(AshOperator.ADD, left, parseTernary())) }
            check(TokenType.MINUS_EQ) -> { advance(); val t = toTarget(left); AssignNode(t, BinOpNode(AshOperator.SUB, left, parseTernary())) }
            check(TokenType.STAR_EQ) -> { advance(); val t = toTarget(left); AssignNode(t, BinOpNode(AshOperator.MUL, left, parseTernary())) }
            check(TokenType.SLASH_EQ) -> { advance(); val t = toTarget(left); AssignNode(t, BinOpNode(AshOperator.DIV, left, parseTernary())) }
            check(TokenType.PERCENT_EQ) -> { advance(); val t = toTarget(left); AssignNode(t, BinOpNode(AshOperator.REM, left, parseTernary())) }
            else -> left
        }
    }

    private fun toTarget(expr: ExprNode): LvalueNode = when (expr) {
        is VarRefNode -> VarLvalue(expr.name)
        is IndexNode -> IndexLvalue(expr.aggregate, expr.index)
        is FieldAccessNode -> FieldLvalue(expr.record, expr.fieldName)
        else -> throw ScriptException("Not an assignable expression", currentLine())
    }

    private fun parseTernary(): ExprNode {
        val cond = parseOr()
        if (!check(TokenType.QUESTION)) return cond
        advance()
        val then = parseOr()
        expect(TokenType.COLON)
        val els = parseOr()
        return TernaryNode(cond, then, els)
    }

    private fun parseOr(): ExprNode {
        var l = parseAnd()
        while (check(TokenType.PIPE_PIPE)) { advance(); l = BinOpNode(AshOperator.OR, l, parseAnd()) }
        return l
    }

    private fun parseAnd(): ExprNode {
        var l = parseBitOr()
        while (check(TokenType.AMP_AMP)) { advance(); l = BinOpNode(AshOperator.AND, l, parseBitOr()) }
        return l
    }

    private fun parseBitOr(): ExprNode {
        var l = parseBitXor()
        while (check(TokenType.PIPE)) { advance(); l = BinOpNode(AshOperator.BOR, l, parseBitXor()) }
        return l
    }

    private fun parseBitXor(): ExprNode {
        var l = parseBitAnd()
        while (check(TokenType.CARET)) { advance(); l = BinOpNode(AshOperator.BXOR, l, parseBitAnd()) }
        return l
    }

    private fun parseBitAnd(): ExprNode {
        var l = parseEquality()
        while (check(TokenType.AMP)) { advance(); l = BinOpNode(AshOperator.BAND, l, parseEquality()) }
        return l
    }

    private fun parseEquality(): ExprNode {
        var l = parseComparison()
        while (true) {
            l = when {
                check(TokenType.EQ_EQ) -> { advance(); BinOpNode(AshOperator.EQ, l, parseComparison()) }
                check(TokenType.BANG_EQ) -> { advance(); BinOpNode(AshOperator.NEQ, l, parseComparison()) }
                else -> return l
            }
        }
    }

    private fun parseComparison(): ExprNode {
        var l = parseAddition()
        while (true) {
            l = when {
                check(TokenType.LT) -> { advance(); BinOpNode(AshOperator.LT, l, parseAddition()) }
                check(TokenType.LE) -> { advance(); BinOpNode(AshOperator.LE, l, parseAddition()) }
                check(TokenType.GT) -> { advance(); BinOpNode(AshOperator.GT, l, parseAddition()) }
                check(TokenType.GE) -> { advance(); BinOpNode(AshOperator.GE, l, parseAddition()) }
                else -> return l
            }
        }
    }

    private fun parseAddition(): ExprNode {
        var l = parseMultiply()
        while (true) {
            l = when {
                check(TokenType.PLUS) -> { advance(); BinOpNode(AshOperator.ADD, l, parseMultiply()) }
                check(TokenType.MINUS) -> { advance(); BinOpNode(AshOperator.SUB, l, parseMultiply()) }
                else -> return l
            }
        }
    }

    private fun parseMultiply(): ExprNode {
        var l = parsePower()
        while (true) {
            l = when {
                check(TokenType.STAR) -> { advance(); BinOpNode(AshOperator.MUL, l, parsePower()) }
                check(TokenType.SLASH) -> { advance(); BinOpNode(AshOperator.DIV, l, parsePower()) }
                check(TokenType.PERCENT) -> { advance(); BinOpNode(AshOperator.REM, l, parsePower()) }
                else -> return l
            }
        }
    }

    private fun parsePower(): ExprNode {
        val base = parseUnary()
        if (!check(TokenType.STAR_STAR)) return base
        advance()
        return BinOpNode(AshOperator.POW, base, parsePower()) // right-associative
    }

    private fun parseUnary(): ExprNode = when {
        check(TokenType.BANG) -> { advance(); UnaryOpNode(AshOperator.NOT, parseUnary()) }
        check(TokenType.MINUS) -> { advance(); UnaryOpNode(AshOperator.NEGATE, parseUnary()) }
        check(TokenType.TILDE) -> { advance(); UnaryOpNode(AshOperator.BNOT, parseUnary()) }
        check(TokenType.PLUS_PLUS) -> { advance(); UnaryOpNode(AshOperator.PRE_INC, parsePostfix()) }
        check(TokenType.MINUS_MINUS) -> { advance(); UnaryOpNode(AshOperator.PRE_DEC, parsePostfix()) }
        else -> parsePostfix()
    }

    private fun parsePostfix(): ExprNode {
        var expr = parsePrimary()
        while (true) {
            expr = when {
                check(TokenType.PLUS_PLUS) -> { advance(); UnaryOpNode(AshOperator.POST_INC, expr) }
                check(TokenType.MINUS_MINUS) -> { advance(); UnaryOpNode(AshOperator.POST_DEC, expr) }
                check(TokenType.LBRACKET) -> {
                    advance(); val idx = parseExpr(); expect(TokenType.RBRACKET)
                    IndexNode(expr, idx)
                }
                check(TokenType.DOT) -> {
                    advance(); val field = expectIdent()
                    FieldAccessNode(expr, field)
                }
                else -> return expr
            }
        }
    }

    private fun parsePrimary(): ExprNode = when {
        check(TokenType.LPAREN) -> { advance(); val e = parseExpr(); expect(TokenType.RPAREN); e }
        check(TokenType.INT_LIT) -> { val v = current().text.toLong(); advance(); LiteralNode(AshValue.of(v)) }
        check(TokenType.FLOAT_LIT) -> { val v = current().text.toDouble(); advance(); LiteralNode(AshValue.of(v)) }
        check(TokenType.BOOL_LIT) -> { val v = current().text == "true"; advance(); LiteralNode(AshValue.of(v)) }
        check(TokenType.STRING_LIT) -> { val v = current().text; advance(); LiteralNode(AshValue.of(v)) }
        check(TokenType.IDENT) -> {
            val name = current().text; advance()
            if (check(TokenType.LPAREN)) {
                advance()
                val args = parseArgList()
                expect(TokenType.RPAREN)
                FunctionCallNode(name, args)
            } else {
                VarRefNode(name)
            }
        }
        else -> throw ScriptException("Unexpected token '${current().text}'", currentLine())
    }

    private fun parseArgList(): List<ExprNode> {
        if (check(TokenType.RPAREN)) return emptyList()
        val args = mutableListOf(parseExpr())
        while (check(TokenType.COMMA)) { advance(); args.add(parseExpr()) }
        return args
    }

    // ──────────────────────────────────────────────────────────────
    // Statement parsing
    // ──────────────────────────────────────────────────────────────

    private fun parseStatement(): ParseTreeNode? = when {
        check(TokenType.IF) -> parseIf()
        check(TokenType.WHILE) -> parseWhile()
        check(TokenType.REPEAT) -> parseRepeat()
        check(TokenType.FOR) -> parseFor()
        check(TokenType.FOREACH) -> parseForeach()
        check(TokenType.TRY) -> parseTry()
        check(TokenType.RETURN) -> parseReturn()
        check(TokenType.BREAK) -> { advance(); expect(TokenType.SEMICOLON); BreakNode }
        check(TokenType.CONTINUE) -> { advance(); expect(TokenType.SEMICOLON); ContinueNode }
        check(TokenType.EXIT) -> { advance(); expect(TokenType.SEMICOLON); ExitNode }
        check(TokenType.RBRACE) || check(TokenType.EOF) -> null
        // var decl: TYPE IDENT (with optional aggregate bracket)
        current().type == TokenType.IDENT && isTypeName(current().text) && checkNext(TokenType.IDENT) -> parseVarDecl()
        current().type == TokenType.IDENT && isTypeName(current().text) && checkNext(TokenType.LBRACKET) -> parseVarDecl()
        else -> { val e = parseExpr(); expect(TokenType.SEMICOLON); ExprStatement(e) }
    }

    private fun parseIf(): IfNode {
        expect(TokenType.IF); expect(TokenType.LPAREN)
        val cond = parseExpr(); expect(TokenType.RPAREN)
        val thenBlock = parseBlock()
        val elseIfs = mutableListOf<Pair<ExprNode, List<ParseTreeNode>>>()
        var elseBlock: List<ParseTreeNode>? = null
        while (check(TokenType.ELSE)) {
            advance()
            if (check(TokenType.IF)) {
                advance(); expect(TokenType.LPAREN)
                val ec = parseExpr(); expect(TokenType.RPAREN)
                elseIfs.add(ec to parseBlock())
            } else { elseBlock = parseBlock(); break }
        }
        return IfNode(cond, thenBlock, elseIfs, elseBlock)
    }

    private fun parseWhile(): WhileNode {
        expect(TokenType.WHILE); expect(TokenType.LPAREN)
        val cond = parseExpr(); expect(TokenType.RPAREN)
        return WhileNode(cond, parseBlock())
    }

    private fun parseRepeat(): RepeatNode {
        expect(TokenType.REPEAT)
        val body = parseBlock()
        expect(TokenType.UNTIL); expect(TokenType.LPAREN)
        val cond = parseExpr(); expect(TokenType.RPAREN); expect(TokenType.SEMICOLON)
        return RepeatNode(body, cond)
    }

    private fun parseFor(): ForNode {
        expect(TokenType.FOR)
        val varName = expectIdent()
        if (!checkIdent("from")) throw ScriptException("Expected 'from' in for loop", currentLine())
        advance()
        val start = parseExpr()
        val ascending = when {
            check(TokenType.TO) || check(TokenType.UPTO) -> { advance(); true }
            check(TokenType.DOWNTO) -> { advance(); false }
            else -> throw ScriptException("Expected 'to', 'upto', or 'downto'", currentLine())
        }
        val end = parseExpr()
        val step = if (check(TokenType.BY)) { advance(); parseExpr() } else null
        return ForNode(varName, start, end, step, ascending)
    }

    private fun parseForeach(): ForEachNode {
        expect(TokenType.FOREACH)
        val keys = mutableListOf(expectIdent())
        while (check(TokenType.COMMA)) { advance(); keys.add(expectIdent()) }
        expect(TokenType.IN)
        val agg = parseExpr()
        return ForEachNode(keys, agg, parseBlock())
    }

    private fun parseTry(): TryNode {
        expect(TokenType.TRY)
        val body = parseBlock()
        val catchBlock = if (check(TokenType.CATCH)) { advance(); parseBlock() } else null
        return TryNode(body, catchBlock)
    }

    private fun parseReturn(): ReturnNode {
        expect(TokenType.RETURN)
        val value = if (!check(TokenType.SEMICOLON)) parseExpr() else null
        expect(TokenType.SEMICOLON)
        return ReturnNode(value)
    }

    private fun parseVarDecl(): VarDeclNode {
        val type = parseTypeToken()
        val name = expectIdent()
        val init = if (check(TokenType.EQ)) { advance(); parseExpr() } else null
        expect(TokenType.SEMICOLON)
        return VarDeclNode(name, type, init)
    }

    private fun parseBlock(): List<ParseTreeNode> {
        expect(TokenType.LBRACE)
        val stmts = mutableListOf<ParseTreeNode>()
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            parseStatement()?.let { stmts.add(it) }
        }
        expect(TokenType.RBRACE)
        return stmts
    }

    // ──────────────────────────────────────────────────────────────
    // Top-level: function defs, record defs, program
    // ──────────────────────────────────────────────────────────────

    private fun isFunctionDef(): Boolean {
        if (current().type != TokenType.IDENT || !isTypeName(current().text)) return false
        // Handle aggregate return type: TYPE[...] IDENT (
        var lookahead = pos + 1
        if (lookahead < tokens.size && tokens[lookahead].type == TokenType.LBRACKET) {
            lookahead++ // skip index type
            while (lookahead < tokens.size && tokens[lookahead].type != TokenType.RBRACKET) lookahead++
            lookahead++ // skip ]
        }
        if (lookahead >= tokens.size || tokens[lookahead].type != TokenType.IDENT) return false
        if (lookahead + 1 >= tokens.size || tokens[lookahead + 1].type != TokenType.LPAREN) return false
        return true
    }

    private fun parseFunctionDef(): FunctionDefNode {
        val returnType = parseTypeToken()
        val name = expectIdent()
        expect(TokenType.LPAREN)
        val params = mutableListOf<Pair<String, AshType>>()
        if (!check(TokenType.RPAREN)) {
            do {
                val pType = parseTypeToken()
                val pName = expectIdent()
                params.add(pName to pType)
            } while (check(TokenType.COMMA).also { if (it) advance() })
        }
        expect(TokenType.RPAREN)
        val body = parseBlock()
        return FunctionDefNode(AshFunction(name, returnType, params, body))
    }

    private fun parseRecordDef(): RecordDefNode {
        expect(TokenType.RECORD)
        val name = expectIdent()
        expect(TokenType.LBRACE)
        val fields = mutableListOf<RecordField>()
        var idx = 0
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            val fieldType = parseTypeToken()
            val fieldName = expectIdent()
            expect(TokenType.SEMICOLON)
            fields.add(RecordField(fieldName, fieldType, idx++))
        }
        expect(TokenType.RBRACE)
        val rt = RecordType(name, fields)
        knownRecords[name.lowercase()] = rt
        return RecordDefNode(rt)
    }

    fun parse(source: String): List<ParseTreeNode> {
        tokens = tokenize(source)
        pos = 0
        val nodes = mutableListOf<ParseTreeNode>()
        while (!check(TokenType.EOF)) {
            when {
                check(TokenType.RECORD) -> nodes.add(parseRecordDef())
                isFunctionDef() -> nodes.add(parseFunctionDef())
                else -> parseStatement()?.let { nodes.add(it) }
            }
        }
        return nodes
    }
}
```

- [ ] **Step 4: Run — verify tests pass**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.ash.AshParserTest"
```

Expected: PASS (all 35 tests)

- [ ] **Step 5: Commit**

```bash
git add shared/src/
git commit -m "feat: AshParser — tokenizer + recursive descent parser for full ASH grammar"
```

---

## Task 5: RuntimeLibrary Stub + AshRuntime

**Files:**
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/RuntimeLibrary.kt`
- Create: `shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/AshRuntime.kt`
- Test: `shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/AshRuntimeTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/AshRuntimeTest.kt
package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class AshRuntimeTest {

    private fun run(src: String): AshRuntime {
        val runtime = AshRuntime(RuntimeLibrary())
        runtime.execute(AshParser().parse(src))
        return runtime
    }

    private fun output(src: String): String = run(src).output.toString().trim()

    // --- Arithmetic ---

    @Test
    fun eval_intAddition() {
        assertEquals("3", output("print(to_string(1 + 2));"))
    }

    @Test
    fun eval_intMultiplication() {
        assertEquals("6", output("print(to_string(2 * 3));"))
    }

    @Test
    fun eval_operatorPrecedence() {
        assertEquals("7", output("print(to_string(1 + 2 * 3));"))
    }

    @Test
    fun eval_floatArithmetic() {
        assertEquals("2.5", output("print(to_string(5.0 / 2.0));"))
    }

    @Test
    fun eval_stringConcatenation() {
        assertEquals("hello world", output("""print("hello" + " " + "world");"""))
    }

    @Test
    fun eval_divisionByZero_throws() {
        assertFails { run("int x = 1 / 0;") }
    }

    // --- Variables ---

    @Test
    fun eval_varDecl_withInit() {
        assertEquals("42", output("int x = 42; print(to_string(x));"))
    }

    @Test
    fun eval_varDecl_defaultValue() {
        assertEquals("0", output("int x; print(to_string(x));"))
    }

    @Test
    fun eval_varAssignment() {
        assertEquals("10", output("int x = 5; x = x * 2; print(to_string(x));"))
    }

    @Test
    fun eval_compoundAssignment_plusEq() {
        assertEquals("8", output("int x = 5; x += 3; print(to_string(x));"))
    }

    @Test
    fun eval_preIncrement() {
        assertEquals("6", output("int x = 5; ++x; print(to_string(x));"))
    }

    @Test
    fun eval_postIncrement_returnsOld() {
        assertEquals("5", output("int x = 5; int y = x++; print(to_string(y));"))
        assertEquals("6", output("int x = 5; x++; print(to_string(x));"))
    }

    // --- Control flow ---

    @Test
    fun eval_if_taken() {
        assertEquals("yes", output("""if (true) { print("yes"); }"""))
    }

    @Test
    fun eval_if_notTaken() {
        assertEquals("", output("""if (false) { print("no"); }"""))
    }

    @Test
    fun eval_ifElse() {
        assertEquals("b", output("""if (false) { print("a"); } else { print("b"); }"""))
    }

    @Test
    fun eval_elseIf() {
        assertEquals(
            "two",
            output("""
                int x = 2;
                if (x == 1) { print("one"); }
                else if (x == 2) { print("two"); }
                else { print("other"); }
            """.trimIndent())
        )
    }

    @Test
    fun eval_while_sumsToFifteen() {
        assertEquals(
            "15",
            output("""
                int sum = 0; int i = 1;
                while (i <= 5) { sum += i; i++; }
                print(to_string(sum));
            """.trimIndent())
        )
    }

    @Test
    fun eval_repeat_until() {
        assertEquals(
            "3",
            output("""
                int x = 0;
                repeat { x++; } until (x >= 3);
                print(to_string(x));
            """.trimIndent())
        )
    }

    @Test
    fun eval_for_ascending() {
        assertEquals(
            "12345",
            output("""
                string s = "";
                for i from 1 to 5 { s += to_string(i); }
                print(s);
            """.trimIndent())
        )
    }

    @Test
    fun eval_for_downto() {
        assertEquals(
            "54321",
            output("""
                string s = "";
                for i from 5 downto 1 { s += to_string(i); }
                print(s);
            """.trimIndent())
        )
    }

    @Test
    fun eval_break_exits_loop() {
        assertEquals(
            "3",
            output("""
                int x = 0;
                while (true) { x++; if (x == 3) { break; } }
                print(to_string(x));
            """.trimIndent())
        )
    }

    @Test
    fun eval_continue_skips_iteration() {
        assertEquals(
            "135",
            output("""
                string s = "";
                for i from 1 to 5 {
                    if (i == 2 || i == 4) { continue; }
                    s += to_string(i);
                }
                print(s);
            """.trimIndent())
        )
    }

    @Test
    fun eval_ternary() {
        assertEquals("yes", output("""print(true ? "yes" : "no");"""))
        assertEquals("no", output("""print(false ? "yes" : "no");"""))
    }

    // --- User-defined functions ---

    @Test
    fun eval_userFunction_add() {
        assertEquals(
            "7",
            output("""
                int add(int a, int b) { return a + b; }
                print(to_string(add(3, 4)));
            """.trimIndent())
        )
    }

    @Test
    fun eval_userFunction_recursive_factorial() {
        assertEquals(
            "120",
            output("""
                int fact(int n) { if (n <= 1) { return 1; } return n * fact(n - 1); }
                print(to_string(fact(5)));
            """.trimIndent())
        )
    }

    @Test
    fun eval_userFunction_void_noReturn() {
        assertEquals(
            "called",
            output("""
                void greet() { print("called"); }
                greet();
            """.trimIndent())
        )
    }

    // --- Aggregates ---

    @Test
    fun eval_aggregate_setGet() {
        assertEquals(
            "hello",
            output("""
                string[int] m;
                m[0] = "hello";
                print(m[0]);
            """.trimIndent())
        )
    }

    @Test
    fun eval_foreach_over_aggregate() {
        assertEquals(
            "abc",
            output("""
                string[int] m;
                m[0] = "a"; m[1] = "b"; m[2] = "c";
                string s = "";
                foreach k, v in m { s += v; }
                print(s);
            """.trimIndent())
        )
    }

    // --- Records ---

    @Test
    fun eval_record_fieldAccess() {
        assertEquals(
            "100",
            output("""
                record Stats { int hp; int mp; }
                Stats s;
                s.hp = 100;
                print(to_string(s.hp));
            """.trimIndent())
        )
    }

    // --- Try/catch ---

    @Test
    fun eval_tryCatch_catchesScriptException() {
        assertEquals(
            "caught",
            output("""
                try { int x = 1 / 0; } catch { print("caught"); }
            """.trimIndent())
        )
    }

    @Test
    fun eval_try_noException_doesNotRunCatch() {
        assertEquals(
            "ok",
            output("""
                try { print("ok"); } catch { print("caught"); }
            """.trimIndent())
        )
    }
}
```

- [ ] **Step 2: Run — verify it fails**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.ash.AshRuntimeTest"
```

Expected: FAIL — `AshRuntime` and `RuntimeLibrary` do not exist yet.

- [ ] **Step 3: Write `RuntimeLibrary.kt` (stub — print + to_string only)**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/RuntimeLibrary.kt
package net.sourceforge.kolmafia.ash

// Phase 5a stub: registers only the functions needed for runtime self-tests.
// Phase 5b replaces this with the full ~50-function implementation.
open class RuntimeLibrary {

    open fun registerAll(scope: AshScope) {
        register(scope, "print", AshType.VOID, listOf("value" to AshType.STRING)) { runtime, args ->
            runtime.print(args[0].toString())
            AshValue.VOID
        }
        register(scope, "to_string", AshType.STRING, listOf("value" to AshType.INT)) { _, args ->
            AshValue.of(args[0].toString())
        }
        register(scope, "to_string", AshType.STRING, listOf("value" to AshType.FLOAT)) { _, args ->
            AshValue.of(args[0].toString())
        }
        register(scope, "to_string", AshType.STRING, listOf("value" to AshType.BOOLEAN)) { _, args ->
            AshValue.of(args[0].toString())
        }
        register(scope, "to_string", AshType.STRING, listOf("value" to AshType.STRING)) { _, args ->
            args[0]
        }
    }

    protected fun register(
        scope: AshScope,
        name: String,
        returnType: AshType,
        params: List<Pair<String, AshType>>,
        impl: (AshRuntime, List<AshValue>) -> AshValue
    ) {
        scope.declareFunction(
            AshFunction(name, returnType, params, body = null, libraryImpl = impl)
        )
    }
}
```

- [ ] **Step 4: Write `AshRuntime.kt`**

```kotlin
// shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/AshRuntime.kt
package net.sourceforge.kolmafia.ash

class AshRuntime(private val library: RuntimeLibrary) {

    enum class ControlFlow { NORMAL, RETURN, BREAK, CONTINUE, EXIT }

    var controlFlow = ControlFlow.NORMAL
    var returnValue: AshValue = AshValue.VOID
    val output = StringBuilder()

    private val globalScope = AshScope().also { library.registerAll(it) }

    fun execute(script: List<ParseTreeNode>): AshValue {
        controlFlow = ControlFlow.NORMAL
        returnValue = AshValue.VOID
        output.clear()
        val scope = globalScope.child()
        // Pass 1: register all top-level function and record defs
        for (node in script) when (node) {
            is FunctionDefNode -> scope.declareFunction(node.function)
            is RecordDefNode -> { /* type registered during parse */ }
            else -> {}
        }
        // Pass 2: execute top-level statements
        for (node in script) {
            if (node is FunctionDefNode || node is RecordDefNode) continue
            executeNode(node, scope)
            if (controlFlow != ControlFlow.NORMAL) break
        }
        return returnValue
    }

    fun executeNode(node: ParseTreeNode, scope: AshScope): AshValue = when (node) {
        is ExprStatement -> { evalExpr(node.expr, scope); AshValue.VOID }
        is VarDeclNode -> {
            val init = node.init?.let { evalExpr(it, scope) } ?: node.type.defaultValue()
            scope.declareVar(node.varName, node.type, init.coerceTo(node.type))
            AshValue.VOID
        }
        is IfNode -> executeIf(node, scope)
        is WhileNode -> executeWhile(node, scope)
        is RepeatNode -> executeRepeat(node, scope)
        is ForNode -> executeFor(node, scope)
        is ForEachNode -> executeForeach(node, scope)
        is TryNode -> executeTry(node, scope)
        is ReturnNode -> {
            returnValue = node.value?.let { evalExpr(it, scope) } ?: AshValue.VOID
            controlFlow = ControlFlow.RETURN
            returnValue
        }
        is BreakNode -> { controlFlow = ControlFlow.BREAK; AshValue.VOID }
        is ContinueNode -> { controlFlow = ControlFlow.CONTINUE; AshValue.VOID }
        is ExitNode -> { controlFlow = ControlFlow.EXIT; AshValue.VOID }
        is FunctionDefNode, is RecordDefNode -> AshValue.VOID
        else -> AshValue.VOID
    }

    private fun executeBlock(block: List<ParseTreeNode>, scope: AshScope): AshValue {
        val child = scope.child()
        var last = AshValue.VOID
        for (node in block) {
            last = executeNode(node, child)
            if (controlFlow != ControlFlow.NORMAL) break
        }
        return last
    }

    private fun executeIf(node: IfNode, scope: AshScope): AshValue {
        if (evalExpr(node.condition, scope).toBoolean()) return executeBlock(node.thenBlock, scope)
        for ((cond, block) in node.elseIfClauses) {
            if (evalExpr(cond, scope).toBoolean()) return executeBlock(block, scope)
        }
        return node.elseBlock?.let { executeBlock(it, scope) } ?: AshValue.VOID
    }

    private fun executeWhile(node: WhileNode, scope: AshScope): AshValue {
        while (evalExpr(node.condition, scope).toBoolean()) {
            executeBlock(node.body, scope)
            when (controlFlow) {
                ControlFlow.BREAK -> { controlFlow = ControlFlow.NORMAL; break }
                ControlFlow.CONTINUE -> controlFlow = ControlFlow.NORMAL
                ControlFlow.NORMAL -> {}
                else -> break
            }
        }
        return AshValue.VOID
    }

    private fun executeRepeat(node: RepeatNode, scope: AshScope): AshValue {
        do {
            executeBlock(node.body, scope)
            when (controlFlow) {
                ControlFlow.BREAK -> { controlFlow = ControlFlow.NORMAL; return AshValue.VOID }
                ControlFlow.CONTINUE -> controlFlow = ControlFlow.NORMAL
                ControlFlow.NORMAL -> {}
                else -> return AshValue.VOID
            }
        } while (!evalExpr(node.condition, scope).toBoolean())
        return AshValue.VOID
    }

    private fun executeFor(node: ForNode, scope: AshScope): AshValue {
        val loopScope = scope.child()
        var cur = evalExpr(node.start, scope).toLong()
        val end = evalExpr(node.end, scope).toLong()
        val step = node.step?.let { evalExpr(it, scope).toLong() }
            ?: if (node.ascending) 1L else -1L
        loopScope.declareVar(node.varName, AshType.INT, AshValue.of(cur))
        while (if (node.ascending) cur <= end else cur >= end) {
            loopScope.setVar(node.varName, AshValue.of(cur))
            executeBlock(node.body, loopScope)
            when (controlFlow) {
                ControlFlow.BREAK -> { controlFlow = ControlFlow.NORMAL; break }
                ControlFlow.CONTINUE -> controlFlow = ControlFlow.NORMAL
                ControlFlow.NORMAL -> {}
                else -> break
            }
            cur += step
        }
        return AshValue.VOID
    }

    private fun executeForeach(node: ForEachNode, scope: AshScope): AshValue {
        val agg = evalExpr(node.aggregate, scope) as? AggregateValue
            ?: throw ScriptException("foreach requires an aggregate")
        val loopScope = scope.child()
        for ((key, value) in agg.map) {
            when (node.keyNames.size) {
                1 -> loopScope.declareVar(node.keyNames[0], agg.type.indexType, key)
                2 -> {
                    loopScope.declareVar(node.keyNames[0], agg.type.indexType, key)
                    loopScope.declareVar(node.keyNames[1], agg.type.dataType, value)
                }
                else -> throw ScriptException("foreach supports at most 2 key variables")
            }
            executeBlock(node.body, loopScope)
            when (controlFlow) {
                ControlFlow.BREAK -> { controlFlow = ControlFlow.NORMAL; break }
                ControlFlow.CONTINUE -> controlFlow = ControlFlow.NORMAL
                ControlFlow.NORMAL -> {}
                else -> break
            }
        }
        return AshValue.VOID
    }

    private fun executeTry(node: TryNode, scope: AshScope): AshValue = try {
        executeBlock(node.body, scope)
    } catch (e: ScriptException) {
        node.catchBlock?.let { executeBlock(it, scope) } ?: AshValue.VOID
    }

    fun evalExpr(expr: ExprNode, scope: AshScope): AshValue = when (expr) {
        is LiteralNode -> expr.value
        is VarRefNode -> scope.getVar(expr.name)?.value
            ?: throw ScriptException("Undefined variable '${expr.name}'")
        is AssignNode -> {
            val v = evalExpr(expr.value, scope)
            applyAssign(expr.target, v, scope)
            v
        }
        is BinOpNode -> expr.op.apply(evalExpr(expr.left, scope), evalExpr(expr.right, scope))
        is UnaryOpNode -> evalUnary(expr, scope)
        is TernaryNode -> if (evalExpr(expr.condition, scope).toBoolean())
            evalExpr(expr.thenExpr, scope) else evalExpr(expr.elseExpr, scope)
        is FunctionCallNode -> callFunction(expr.name, expr.args.map { evalExpr(it, scope) }, scope)
        is IndexNode -> {
            val agg = evalExpr(expr.aggregate, scope) as? AggregateValue
                ?: throw ScriptException("Cannot index non-aggregate")
            agg[evalExpr(expr.index, scope)]
        }
        is FieldAccessNode -> {
            val obj = evalExpr(expr.record, scope)
            when (obj) {
                is RecordValue -> obj.getField(expr.fieldName)
                is AggregateValue -> when (expr.fieldName) {
                    "length", "size", "count" -> obj.size()
                    else -> throw ScriptException("Aggregate has no field '${expr.fieldName}'")
                }
                else -> throw ScriptException("Cannot access field '${expr.fieldName}' on ${obj.type.name}")
            }
        }
        is AggregateLiteralNode -> {
            val result = AggregateValue(expr.aggType)
            for ((keyExpr, valueExpr) in expr.entries) {
                val key = keyExpr?.let { evalExpr(it, scope) } ?: AshValue.of(result.map.size)
                result[key] = evalExpr(valueExpr, scope)
            }
            result
        }
        is RecordLiteralNode -> {
            val result = RecordValue(expr.recordType)
            expr.fieldExprs.forEachIndexed { i, fe -> fe?.let { result.setField(i, evalExpr(it, scope)) } }
            result
        }
    }

    private fun evalUnary(expr: UnaryOpNode, scope: AshScope): AshValue {
        val operand = evalExpr(expr.operand, scope)
        val result = expr.op.applyUnary(operand)
        // Write back for increment/decrement operators
        val target: LvalueNode? = when (expr.operand) {
            is VarRefNode -> VarLvalue(expr.operand.name)
            is IndexNode -> IndexLvalue(expr.operand.aggregate, expr.operand.index)
            else -> null
        }
        if (target != null && (expr.op == AshOperator.PRE_INC || expr.op == AshOperator.PRE_DEC ||
                               expr.op == AshOperator.POST_INC || expr.op == AshOperator.POST_DEC)) {
            applyAssign(target, result, scope)
        }
        return if (expr.op == AshOperator.POST_INC || expr.op == AshOperator.POST_DEC) operand else result
    }

    private fun applyAssign(target: LvalueNode, value: AshValue, scope: AshScope) {
        when (target) {
            is VarLvalue -> scope.setVar(target.name, value)
            is IndexLvalue -> {
                val agg = evalExpr(target.aggregate, scope) as? AggregateValue
                    ?: throw ScriptException("Cannot index-assign non-aggregate")
                agg[evalExpr(target.index, scope)] = value
            }
            is FieldLvalue -> {
                val rec = evalExpr(target.record, scope) as? RecordValue
                    ?: throw ScriptException("Cannot field-assign non-record")
                rec.setField(target.fieldName, value)
            }
        }
    }

    private fun callFunction(name: String, args: List<AshValue>, scope: AshScope): AshValue {
        val fn = scope.resolveFunction(name, args.map { it.type })
            ?: throw ScriptException(
                "No matching overload of '$name' for (${args.joinToString { it.type.name }})"
            )
        return if (fn.libraryImpl != null) {
            fn.libraryImpl.invoke(this, args)
        } else {
            val fnScope = globalScope.child()
            fn.params.forEachIndexed { i, (pName, pType) ->
                fnScope.declareVar(pName, pType, args[i].coerceTo(pType))
            }
            val savedCF = controlFlow; val savedRV = returnValue
            controlFlow = ControlFlow.NORMAL
            returnValue = fn.returnType.defaultValue()
            executeBlock(fn.body!!, fnScope)
            val result = returnValue
            if (controlFlow == ControlFlow.RETURN) controlFlow = ControlFlow.NORMAL
            returnValue = savedRV
            result
        }
    }

    fun print(msg: String) { output.append(msg).append('\n') }
}
```

- [ ] **Step 5: Run — verify tests pass**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.ash.AshRuntimeTest"
```

Expected: PASS (all tests)

- [ ] **Step 6: Run all ASH tests**

```bash
./gradlew :shared:jvmTest --tests "net.sourceforge.kolmafia.ash.*"
```

Expected: PASS (AshTypeTest + AshParserTest + AshRuntimeTest — all tests green)

- [ ] **Step 7: Run the full shared test suite — no regressions**

```bash
./gradlew :shared:jvmTest
```

Expected: All previously passing tests PASS.

- [ ] **Step 8: Commit**

```bash
git add shared/src/
git commit -m "feat: AshRuntime execution engine + RuntimeLibrary stub (print, to_string)"
```

---

## Self-Review

**Spec coverage:**

| Phase 5 deliverable | Task |
|---|---|
| ASH interpreter ported to Kotlin (commonMain) | Tasks 1–5 |
| Type system: void, boolean, int, float, string, buffer, 13 game entity types | Task 1 |
| Aggregate types (map/array) and record types | Task 1 |
| All AST node types: if/else-if/else, while, repeat-until, for, foreach, try/catch, return, break, continue, exit | Tasks 2, 4 |
| Recursive descent parser — full ASH grammar | Task 4 |
| Operator precedence: 8 levels (or < and < bitor < bitxor < bitand < equality < comparison < addition < multiply < power < unary < postfix) | Tasks 3, 4 |
| User-defined functions with overloading | Tasks 2, 5 |
| Variable scoping (lexical, chained parent scope) | Task 2 |
| Execution engine: all statement + expression types | Task 5 |
| Aggregate and record values with runtime get/set | Tasks 1, 5 |
| foreach over aggregates (1 key, 2 keys) | Tasks 2, 5 |
| try/catch for ScriptException | Tasks 1, 5 |

**Placeholder scan:** No TBD, TODO, or "implement later" patterns. Every step contains complete code.

**Type consistency check:**
- `AshType.defaultValue()` defined in Task 1, called in `VarDeclNode` handler in Task 5 — ✓
- `AggregateValue` defined in Task 1, created in Task 5 `executeNode` for `VarDeclNode` — ✓
- `AshFunction.libraryImpl` defined in Task 2, invoked in Task 5 `callFunction` — ✓
- `AshOperator.PRE_INC` / `POST_INC` defined in Task 3, handled in Task 5 `evalUnary` — ✓
- `ForNode.ascending` defined in Task 2, used in Task 5 `executeFor` — ✓
- All `ParseTreeNode` subtypes defined in Task 2, exhaustively handled in Task 5 `evalExpr` and `executeNode` — ✓

**Note:** The `RuntimeLibrary` in this plan is a stub providing only `print()` and `to_string()`. Phase 5b replaces it with a full subclass that registers ~50 game-facing built-in functions. The `RuntimeLibrary` class is `open` and `registerAll` is `open` specifically to allow Phase 5b's subclass to call `super.registerAll()` and add its own functions.
