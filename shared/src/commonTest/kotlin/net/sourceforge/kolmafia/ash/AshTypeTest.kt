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
        assertEquals(0L, v[AshValue.of("bar")].toLong())
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
