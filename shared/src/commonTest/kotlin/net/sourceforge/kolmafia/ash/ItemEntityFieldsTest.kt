package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.RestoreDatabase
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.modifiers.ExpressionContext

class ItemEntityFieldsTest {

    private lateinit var db: GameDatabase

    @BeforeTest
    fun setUp() = runTest {
        db = GameDatabase()
        db.load()
    }

    @AfterTest
    fun tearDown() {
        ConsumableDatabase.resetForTest()
        RestoreDatabase.resetForTest()
    }

    @Test
    fun levelreq_foodAndNonfilling() {
        assertEquals(1L, ItemEntityFields.resolve("acceptable bagel", "levelreq", db).toLong())
        assertEquals(0L, ItemEntityFields.resolve("battery (AAA)", "levelreq", db).toLong())
        assertEquals(13L, ItemEntityFields.resolve("mime army challenge coin", "levelreq", db).toLong())
    }

    @Test
    fun fullness_foodVsNonfilling() {
        assertEquals(3L, ItemEntityFields.resolve("acceptable bagel", "fullness", db).toLong())
        assertEquals(0L, ItemEntityFields.resolve("battery (AAA)", "fullness", db).toLong())
    }

    @Test
    fun inebriety_andSpleen() {
        assertEquals(6L, ItemEntityFields.resolve("Lucky Lindy", "inebriety", db).toLong())
        assertEquals(1L, ItemEntityFields.resolve("a bug's lymph", "spleen", db).toLong())
    }

    @Test
    fun quality_andAdventures() {
        assertEquals("good", ItemEntityFields.resolve("acceptable bagel", "quality", db).toString())
        assertEquals("8-9", ItemEntityFields.resolve("acceptable bagel", "adventures", db).toString())
        assertEquals("16-20", ItemEntityFields.resolve("Lucky Lindy", "adventures", db).toString())
    }

    @Test
    fun id_andPlural_fromItemDatabase() {
        assertEquals(8196L, ItemEntityFields.resolve("acceptable bagel", "id", db).toLong())
        assertEquals("acceptable bagels", ItemEntityFields.resolve("acceptable bagel", "plural", db).toString())
        assertEquals(7592L, ItemEntityFields.resolve("Lucky Lindy", "id", db).toLong())
    }

    @Test
    fun restore_minhpMaxhpAndMp() {
        val ctx = ExpressionContext.EMPTY
        assertEquals(101L, ItemEntityFields.resolve("aspirin", "minhp", db, ctx).toLong())
        assertEquals(101L, ItemEntityFields.resolve("aspirin", "maxhp", db, ctx).toLong())
        assertEquals(30L, ItemEntityFields.resolve("ancient pills", "minmp", db, ctx).toLong())
        assertEquals(40L, ItemEntityFields.resolve("ancient pills", "maxmp", db, ctx).toLong())
    }

    @Test
    fun restore_batteryMpPathDependent() {
        val offPath = ExpressionContext(challengePath = "")
        assertEquals(30L, ItemEntityFields.resolve("battery (AAA)", "maxmp", db, offPath).toLong())
        val onPath = ExpressionContext(challengePath = AscensionPath.YOU_ROBOT.apiName)
        assertEquals(0L, ItemEntityFields.resolve("battery (AAA)", "maxmp", db, onPath).toLong())
    }

    @Test
    fun flag_accessAndUsability() {
        assertEquals(true, ItemEntityFields.resolve("acceptable bagel", "tradeable", db).toBoolean())
        assertEquals(true, ItemEntityFields.resolve("acceptable bagel", "discardable", db).toBoolean())
        assertEquals(true, ItemEntityFields.resolve("Dolphin King's map", "quest", db).toBoolean())
        assertEquals(false, ItemEntityFields.resolve("acceptable bagel", "quest", db).toBoolean())
        assertEquals(true, ItemEntityFields.resolve("ten-leaf clover", "multi", db).toBoolean())
        assertEquals(true, ItemEntityFields.resolve("spider web", "combat", db).toBoolean())
        assertEquals(false, ItemEntityFields.resolve("spider web", "usable", db).toBoolean())
        assertEquals(true, ItemEntityFields.resolve("seal tooth", "combat_reusable", db).toBoolean())
        assertEquals(true, ItemEntityFields.resolve("seal tooth", "combat", db).toBoolean())
        assertEquals(true, ItemEntityFields.resolve("seal-clubbing club", "pasteable", db).toBoolean())
        assertEquals(true, ItemEntityFields.resolve("seal-clubbing club", "smithable", db).toBoolean())
    }

    @Test
    fun metadata_notesAndCandyFields() {
        assertEquals("Unspaded", ItemEntityFields.resolve("candy rations", "notes", db).toString())
        assertEquals(true, ItemEntityFields.resolve("tamarind-flavored chewing gum", "potion", db).toBoolean())
        assertEquals(true, ItemEntityFields.resolve("tamarind-flavored chewing gum", "candy", db).toBoolean())
        assertEquals("simple", ItemEntityFields.resolve("tamarind-flavored chewing gum", "candy_type", db).toString())
        assertEquals("complex", ItemEntityFields.resolve("Now and Earlier", "candy_type", db).toString())
        assertEquals(true, ItemEntityFields.resolve("fancy chocolate", "chocolate", db).toBoolean())
        assertEquals("unspaded", ItemEntityFields.resolve("hard rock candy", "candy_type", db).toString())
        assertEquals(7L, ItemEntityFields.resolve("aspirin", "name_length", db).toLong())
    }

    @Test
    fun unknownField_throws() {
        assertFailsWith<ScriptException> {
            ItemEntityFields.resolve("acceptable bagel", "not_a_field", db)
        }
    }

    @Test
    fun dailyUsesLeft_returnsMaximumUses() {
        assertEquals(
            Int.MAX_VALUE.toLong(),
            ItemEntityFields.resolve("ten-leaf clover", "dailyusesleft", db).toLong(),
        )
    }
}
