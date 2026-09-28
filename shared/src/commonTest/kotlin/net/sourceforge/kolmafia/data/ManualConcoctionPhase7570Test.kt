package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState

/** MANUAL concoction edges residual (phases 7511–7570). */
class ManualConcoctionPhase7570Test {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun parse_setsCraftYieldFromResultQuantity() {
        ConcoctionDatabase.parseForTest("bottle of gin (3)\tMIX\tfermenting powder\tjuniper berries")
        val gin = ConcoctionDatabase.getByResult("bottle of gin")!!
        assertEquals(3, gin.resultQuantity)
        assertEquals(3, gin.craftYield)
        assertEquals(3, ConcoctionDatabase.getYield("bottle of gin", tripleReagent = false))
    }

    @Test
    fun parse_skipsManualRows() {
        ConcoctionDatabase.parseForTest(
            """
            green beer	MIX, MANUAL	ten-leaf clover	ice-cold Sir Schlitz
            widget	COMBINE	a	b
            """.trimIndent(),
        )
        assertNull(ConcoctionDatabase.getByResult("green beer"))
        assertEquals(setOf("COMBINE"), ConcoctionDatabase.getByResult("widget")!!.methods)
    }

    @Test
    fun parse_firstNonManualWins() {
        ConcoctionDatabase.parseForTest(
            """
            widget	COMBINE	a	b
            widget	COOK	c	d
            """.trimIndent(),
        )
        val widget = ConcoctionDatabase.getByResult("widget")!!
        assertEquals(setOf("COMBINE"), widget.methods)
        assertEquals("a", widget.ingredients[0].name)
    }

    @Test
    fun parse_manualDoesNotBlockLaterRealRecipe() {
        ConcoctionDatabase.parseForTest(
            """
            green beer	MIX, MANUAL	ten-leaf clover	ice-cold Sir Schlitz
            green beer	MIX	water	hops
            """.trimIndent(),
        )
        val beer = ConcoctionDatabase.getByResult("green beer")!!
        assertEquals(setOf("MIX"), beer.methods)
        assertEquals("water", beer.ingredients[0].name)
        assertTrue(!beer.isManual)
    }

    @Test
    fun creatable_sx3TriplesSaucerorYield() {
        registerItems(
            9101 to "sx3 potion",
            9102 to "scrumptious reagent",
            9103 to "sx3 fruit",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "scrumptious reagent",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "sx3 fruit",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "sx3 potion",
                resultQuantity = 1,
                methods = setOf("SAUCE", "SX3"),
                ingredients = listOf(
                    ConcoctionIngredient("scrumptious reagent", 1),
                    ConcoctionIngredient("sx3 fruit", 1),
                ),
            ),
        )
        val counts = mapOf(9102 to 3, 9103 to 3)
        val sauceror = CharacterState(characterClass = CharacterClass.SAUCEROR.id)
        val context = ConcoctionCreatableContext(
            initialCount = { name ->
                when (name.lowercase()) {
                    "scrumptious reagent" -> 3
                    "sx3 fruit" -> 3
                    else -> 0
                }
            },
            availableCountById = { id -> counts[id] ?: 0 },
            tripleReagent = sauceror.isSauceror,
        )
        val concoction = ConcoctionDatabase.getByResult("sx3 potion")!!
        assertEquals(9, calculateCreatableTotal(concoction, context))
        val mundane = context.copy(tripleReagent = false)
        assertEquals(3, calculateCreatableTotal(concoction, mundane))
    }

    @Test
    fun recipeCli_flattensMissingChildAndShowsYield() {
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "cli dough",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("cli flour", 1)),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "cli bread",
                resultQuantity = 1,
                methods = setOf("COOK"),
                ingredients = listOf(ConcoctionIngredient("cli dough", 1)),
            ),
        )
        val missing = ConcoctionRecipeCli.formatIngredients(
            ConcoctionDatabase.getByResult("cli bread")!!,
            haveCount = { 0 },
            tripleReagent = false,
        )
        assertTrue(missing.contains("cli flour"), missing)
        assertTrue(!missing.contains("cli dough") || missing.contains("cli flour"), missing)

        val potion = ConcoctionData(
            result = "sx3 potion",
            resultQuantity = 1,
            methods = setOf("SAUCE", "SX3"),
            ingredients = listOf(ConcoctionIngredient("reagent", 1)),
        )
        assertEquals("3 sx3 potion", ConcoctionRecipeCli.formatYieldName(potion, tripleReagent = true))
    }

    private fun registerItems(vararg items: Pair<Int, String>) {
        for ((id, name) in items) {
            ItemDatabase.registerForTest(
                ItemData(
                    id = id,
                    name = name,
                    descId = name.replace(' ', '_'),
                    image = "img.gif",
                    primaryUse = ItemPrimaryUse.NONE,
                    secondaryUses = emptySet(),
                    access = setOf('t', 'd'),
                    autosellPrice = 100,
                    plural = null,
                ),
            )
        }
    }
}
