package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConcoctionCreatableTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun calculateCreatableTotal_flatTwoIngredientCombine() {
        registerItems(
            7001 to "creatable result",
            7002 to "creatable ing a",
            7003 to "creatable ing b",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "creatable ing a",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "creatable ing b",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "creatable result",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(
                    ConcoctionIngredient("creatable ing a", 2),
                    ConcoctionIngredient("creatable ing b", 2),
                ),
            ),
        )

        val context = ConcoctionCreatableContext.fromRuntime { name ->
            when (name.lowercase()) {
                "creatable ing a" -> 6
                "creatable ing b" -> 6
                else -> 0
            }
        }
        val concoction = ConcoctionDatabase.getByResult("creatable result")!!
        assertEquals(3, calculateCreatableTotal(concoction, context))
    }

    @Test
    fun calculateCreatableTotal_scarcestIngredientLimit() {
        registerItems(
            7101 to "scarce result",
            7102 to "scarce ing a",
            7103 to "scarce ing b",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "scarce ing a",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "scarce ing b",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "scarce result",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(
                    ConcoctionIngredient("scarce ing a", 2),
                    ConcoctionIngredient("scarce ing b", 3),
                ),
            ),
        )

        val context = ConcoctionCreatableContext.fromRuntime { name ->
            when (name.lowercase()) {
                "scarce ing a" -> 10
                "scarce ing b" -> 9
                else -> 0
            }
        }
        val concoction = ConcoctionDatabase.getByResult("scarce result")!!
        assertEquals(3, calculateCreatableTotal(concoction, context))
    }

    @Test
    fun calculateCreatableTotal_onHandInitialPlusCreatable() {
        registerItems(
            7201 to "onhand parent",
            7202 to "onhand leaf",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "onhand leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "onhand parent",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(ConcoctionIngredient("onhand leaf", 1)),
            ),
        )

        val context = ConcoctionCreatableContext.fromRuntime { name ->
            when (name.lowercase()) {
                "onhand parent" -> 2
                "onhand leaf" -> 5
                else -> 0
            }
        }
        val concoction = ConcoctionDatabase.getByResult("onhand parent")!!
        assertEquals(7, calculateCreatableTotal(concoction, context))
    }

    @Test
    fun calculateCreatableTotal_nestedSmithChildInCloset() {
        registerItems(
            7301 to "nested parent",
            7302 to "nested child",
            7303 to "nested leaf",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "nested leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "nested child",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(ConcoctionIngredient("nested leaf", 1)),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "nested parent",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(
                    ConcoctionIngredient("nested child", 1),
                    ConcoctionIngredient("nested leaf", 1),
                ),
            ),
        )

        val context = ConcoctionCreatableContext.fromRuntime { name ->
            when (name.lowercase()) {
                "nested leaf" -> 2
                else -> 0
            }
        }
        val concoction = ConcoctionDatabase.getByResult("nested parent")!!
        val total = calculateCreatableTotal(concoction, context)
        assertTrue(total > 0, "nested parent should be creatable when leaf is on hand")
        assertEquals(1, total)
    }

    @Test
    fun calculateCreatableTotal_noCreateEmptyIngredients() {
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "leaf only",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )

        val context = ConcoctionCreatableContext.fromRuntime { name ->
            if (name.equals("leaf only", ignoreCase = true)) 4 else 0
        }
        val concoction = ConcoctionDatabase.getByResult("leaf only")!!
        assertEquals(4, calculateCreatableTotal(concoction, context))
    }

    @Test
    fun calculateCreatableTotal_creationLoopDoesNotHang() {
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "loop a",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("loop b", 1)),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "loop b",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("loop a", 1)),
            ),
        )

        val context = ConcoctionCreatableContext.fromRuntime { 0 }
        val concoction = ConcoctionDatabase.getByResult("loop a")!!
        val total = calculateCreatableTotal(concoction, context)
        assertEquals(0, total)
    }

    @Test
    fun refreshConcoctionsNow_populatesCreatableAndTotal() {
        registerItems(
            7401 to "refresh parent",
            7402 to "refresh leaf",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "refresh leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "refresh parent",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(ConcoctionIngredient("refresh leaf", 1)),
            ),
        )

        val context = ConcoctionRefreshContext.fromAggregatedCounts(mapOf(7402 to 3))
        ConcoctionDatabase.refreshConcoctionsNow(context)

        assertEquals(0, ConcoctionDatabase.initialCount("refresh parent"))
        assertEquals(3, ConcoctionDatabase.creatableCount("refresh parent"))
        assertEquals(3, ConcoctionDatabase.totalCount("refresh parent"))
        assertEquals(3, ConcoctionDatabase.initialCount("refresh leaf"))
        assertEquals(0, ConcoctionDatabase.creatableCount("refresh leaf"))
    }

    @Test
    fun calculateCreatableTotal_smithZeroAdventures_creatableBlocked() {
        registerItems(
            7501 to "adv smith parent",
            7502 to "adv smith leaf",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "adv smith leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "adv smith parent",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(ConcoctionIngredient("adv smith leaf", 1)),
            ),
        )

        val limitPools = ConcoctionLimitPools.forTest(
            adventureLimit = 0,
            adventureSmithingLimit = 0,
            cookingLimit = 0,
            cocktailcraftingLimit = 0,
            turnFreeLimit = 0,
            turnFreeSmithingLimit = 0,
            turnFreeCookingLimit = 0,
            turnFreeCocktailcraftingLimit = 0,
        )
        val context = ConcoctionCreatableContext.fromRuntime { name ->
            when (name.lowercase()) {
                "adv smith leaf" -> 5
                else -> 0
            }
        }.copy(limitPools = limitPools)

        val concoction = ConcoctionDatabase.getByResult("adv smith parent")!!
        assertEquals(0, calculateCreatableTotal(concoction, context))
    }

    @Test
    fun calculateCreatableFreeTotal_smithFreeTurnsOnly() {
        registerItems(
            7601 to "free smith parent",
            7602 to "free smith leaf",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "free smith leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "free smith parent",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(ConcoctionIngredient("free smith leaf", 1)),
            ),
        )

        val limitPools = ConcoctionLimitPools.forTest(
            adventureLimit = 0,
            adventureSmithingLimit = 0,
            cookingLimit = 0,
            cocktailcraftingLimit = 0,
            turnFreeLimit = 0,
            turnFreeSmithingLimit = 3,
            turnFreeCookingLimit = 0,
            turnFreeCocktailcraftingLimit = 0,
        )
        val context = ConcoctionCreatableContext.fromRuntime { name ->
            when (name.lowercase()) {
                "free smith leaf" -> 5
                else -> 0
            }
        }.copy(limitPools = limitPools)

        val concoction = ConcoctionDatabase.getByResult("free smith parent")!!
        assertEquals(0, calculateCreatableTotal(concoction, context))
        assertEquals(3, calculateCreatableFreeTotal(concoction, context))
    }

    @Test
    fun calculateCreatableTotal_combineUnaffectedByZeroAdventurePools() {
        registerItems(
            7701 to "adv combine result",
            7702 to "adv combine leaf",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "adv combine leaf",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "adv combine result",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("adv combine leaf", 2)),
            ),
        )

        val limitPools = ConcoctionLimitPools.forTest(
            adventureLimit = 0,
            adventureSmithingLimit = 0,
            cookingLimit = 0,
            cocktailcraftingLimit = 0,
            turnFreeLimit = 0,
            turnFreeSmithingLimit = 0,
            turnFreeCookingLimit = 0,
            turnFreeCocktailcraftingLimit = 0,
        )
        val context = ConcoctionCreatableContext.fromRuntime { name ->
            when (name.lowercase()) {
                "adv combine leaf" -> 6
                else -> 0
            }
        }.copy(limitPools = limitPools)

        val concoction = ConcoctionDatabase.getByResult("adv combine result")!!
        assertEquals(3, calculateCreatableTotal(concoction, context))
    }

    @Test
    fun calculateCreatableTotal_stillZeroStillsLimit_creatableBlocked() {
        registerItems(
            7801 to "still parent",
            7802 to "still leaf",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "still leaf",
                resultQuantity = 1,
                methods = setOf("STILL"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "still parent",
                resultQuantity = 1,
                methods = setOf("STILL"),
                ingredients = listOf(ConcoctionIngredient("still leaf", 1)),
            ),
        )

        val limitPools = ConcoctionLimitPools.forTest(stillsLimit = 0)
        val context = ConcoctionCreatableContext.fromRuntime { name ->
            when (name.lowercase()) {
                "still leaf" -> 5
                else -> 0
            }
        }.copy(limitPools = limitPools)

        val concoction = ConcoctionDatabase.getByResult("still parent")!!
        assertEquals(0, calculateCreatableTotal(concoction, context))
    }

    @Test
    fun calculateCreatableTotal_stillStillsLimitCapsCreatable() {
        registerItems(
            7811 to "still cap parent",
            7812 to "still cap leaf",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "still cap leaf",
                resultQuantity = 1,
                methods = setOf("STILL"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "still cap parent",
                resultQuantity = 1,
                methods = setOf("STILL"),
                ingredients = listOf(ConcoctionIngredient("still cap leaf", 1)),
            ),
        )

        val limitPools = ConcoctionLimitPools.forTest(stillsLimit = 2)
        val context = ConcoctionCreatableContext.fromRuntime { name ->
            when (name.lowercase()) {
                "still cap leaf" -> 5
                else -> 0
            }
        }.copy(limitPools = limitPools)

        val concoction = ConcoctionDatabase.getByResult("still cap parent")!!
        assertEquals(2, calculateCreatableTotal(concoction, context))
    }

    @Test
    fun calculateCreatableTotal_clipArtZeroLimit_creatableBlocked() {
        registerItems(
            7821 to "clip parent",
            7822 to "clip leaf",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "clip leaf",
                resultQuantity = 1,
                methods = setOf("CLIPART"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "clip parent",
                resultQuantity = 1,
                methods = setOf("CLIPART"),
                ingredients = listOf(ConcoctionIngredient("clip leaf", 1)),
            ),
        )

        val limitPools = ConcoctionLimitPools.forTest(clipArtLimit = 0)
        val context = ConcoctionCreatableContext.fromRuntime { name ->
            when (name.lowercase()) {
                "clip leaf" -> 5
                else -> 0
            }
        }.copy(limitPools = limitPools)

        val concoction = ConcoctionDatabase.getByResult("clip parent")!!
        assertEquals(0, calculateCreatableTotal(concoction, context))
    }

    @Test
    fun calculateCreatableTotal_terminalZeroExtrudeLimit_creatableBlocked() {
        registerItems(
            7831 to "term parent",
            7832 to "term leaf",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "term leaf",
                resultQuantity = 1,
                methods = setOf("TERMINAL"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "term parent",
                resultQuantity = 1,
                methods = setOf("TERMINAL"),
                ingredients = listOf(ConcoctionIngredient("term leaf", 1)),
            ),
        )

        val limitPools = ConcoctionLimitPools.forTest(extrudeLimit = 0)
        val context = ConcoctionCreatableContext.fromRuntime { name ->
            when (name.lowercase()) {
                "term leaf" -> 5
                else -> 0
            }
        }.copy(limitPools = limitPools)

        val concoction = ConcoctionDatabase.getByResult("term parent")!!
        assertEquals(0, calculateCreatableTotal(concoction, context))
    }

    @Test
    fun calculateCreatableTotal_meatLimitCapsPricedBuyable() {
        registerItems(7901 to "priced buyable")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "priced buyable",
                resultQuantity = 1,
                methods = setOf("NOCREATE"),
                ingredients = emptyList(),
            ),
        )

        val limitPools = ConcoctionLimitPools.forTest(meatLimit = 500)
        val context = ConcoctionCreatableContext.fromRuntime { 0 }
            .copy(limitPools = limitPools, priceFor = { 100 })

        val concoction = ConcoctionDatabase.getByResult("priced buyable")!!
        assertEquals(5, calculateCreatableTotal(concoction, context))
    }

    @Test
    fun adjustCreatableForMeatPrice_subtractsAffordableNpcBuys() {
        registerItems(7911 to "npc priced buy")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "npc priced buy",
                resultQuantity = 1,
                methods = setOf("NOCREATE"),
                ingredients = emptyList(),
            ),
        )

        val limitPools = ConcoctionLimitPools.forTest(meatLimit = 500)
        val context = ConcoctionCreatableContext.fromRuntime { 0 }
            .copy(limitPools = limitPools, priceFor = { 100 })

        val concoction = ConcoctionDatabase.getByResult("npc priced buy")!!
        assertEquals(5, calculateCreatableTotal(concoction, context))
        assertEquals(0, adjustCreatableForMeatPrice(5, 0, concoction, context))
    }

    @Test
    fun calculateCreatableTotal_combineWithoutKnoll_needsMeatPaste() {
        registerItems(
            7921 to "paste combine result",
            7922 to "paste combine leaf",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "meat paste",
                resultQuantity = 1,
                methods = setOf("NOCREATE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "paste combine leaf",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "paste combine result",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("paste combine leaf", 1)),
            ),
        )

        val limitPools = ConcoctionLimitPools.forTest(meatLimit = 0)
        val context = ConcoctionCreatableContext.fromRuntime { name ->
            when (name.lowercase()) {
                "paste combine leaf" -> 5
                else -> 0
            }
        }.copy(
            limitPools = limitPools,
            priceFor = { concoction ->
                if (concoction.result.equals("meat paste", ignoreCase = true)) 10 else 0
            },
            knollAvailable = false,
        )

        val concoction = ConcoctionDatabase.getByResult("paste combine result")!!
        assertEquals(0, calculateCreatableTotal(concoction, context))
    }

    @Test
    fun calculateCreatableTotal_queuedMeatSpent_reducesPricedCreatable() {
        registerItems(7931 to "queued meat buy")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "queued meat buy",
                resultQuantity = 1,
                methods = setOf("NOCREATE"),
                ingredients = emptyList(),
            ),
        )

        val withoutQueue = ConcoctionLimitPools.forTest(meatLimit = 500)
        val withQueue = ConcoctionLimitPools.forTest(meatLimit = 500, meatSpent = 200)
        val baseContext = ConcoctionCreatableContext.fromRuntime { 0 }
            .copy(priceFor = { 100 })

        val concoction = ConcoctionDatabase.getByResult("queued meat buy")!!
        val unqueued = calculateCreatableTotal(
            concoction,
            baseContext.copy(limitPools = withoutQueue),
        )
        val queued = calculateCreatableTotal(
            concoction,
            baseContext.copy(limitPools = withQueue),
        )

        assertEquals(5, unqueued)
        assertEquals(3, queued)
    }

    @Test
    fun calculateCreatableTotal_queuedStillsUsed_capsStillConcoction() {
        registerItems(
            7941 to "queued still parent",
            7942 to "queued still leaf",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "queued still leaf",
                resultQuantity = 1,
                methods = setOf("STILL"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "queued still parent",
                resultQuantity = 1,
                methods = setOf("STILL"),
                ingredients = listOf(ConcoctionIngredient("queued still leaf", 1)),
            ),
        )

        val limitPools = ConcoctionLimitPools.forTest(stillsLimit = 2, stillsUsed = 2)
        val context = ConcoctionCreatableContext.fromRuntime { name ->
            when (name.lowercase()) {
                "queued still leaf" -> 5
                else -> 0
            }
        }.copy(limitPools = limitPools)

        val concoction = ConcoctionDatabase.getByResult("queued still parent")!!
        assertEquals(0, calculateCreatableTotal(concoction, context))
    }

    @Test
    fun calculateCreatableTotal_queuedAdventuresUsed_capsSmithConcoction() {
        registerItems(
            7951 to "queued adv smith parent",
            7952 to "queued adv smith leaf",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "queued adv smith leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "queued adv smith parent",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(ConcoctionIngredient("queued adv smith leaf", 1)),
            ),
        )

        val limitPools = ConcoctionLimitPools.forTest(
            adventureSmithingLimit = 10,
            adventuresUsed = 10,
        )
        val context = ConcoctionCreatableContext.fromRuntime { name ->
            when (name.lowercase()) {
                "queued adv smith leaf" -> 5
                else -> 0
            }
        }.copy(limitPools = limitPools)

        val concoction = ConcoctionDatabase.getByResult("queued adv smith parent")!!
        assertEquals(0, calculateCreatableTotal(concoction, context))
    }

    @Test
    fun calculateCreatableTotal_interchangeableIngredient_prefersWillerWhenOnlyWillerAvailable() {
        registerItems(
            41 to "schlitz",
            81 to "willer",
            7961 to "schlitz cocktail",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "schlitz",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "willer",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "schlitz cocktail",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("schlitz", 1)),
            ),
        )

        val context = ConcoctionCreatableContext(
            initialCount = { name ->
                when (name.lowercase()) {
                    "willer", "schlitz" -> 4
                    else -> 0
                }
            },
            availableCountById = { id ->
                when (id) {
                    41, 81 -> 4
                    else -> 0
                }
            },
        )

        val concoction = ConcoctionDatabase.getByResult("schlitz cocktail")!!
        assertEquals(4, calculateCreatableTotal(concoction, context))
    }

    @Test
    fun calculateCreatableTotal_craftQueuePush_reducesThenPopRestores() {
        registerItems(
            8161 to "queue drop leaf",
            8162 to "queue drop parent",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "queue drop leaf",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "queue drop parent",
                resultQuantity = 1,
                methods = setOf("SMITH"),
                ingredients = listOf(ConcoctionIngredient("queue drop leaf", 1)),
            ),
        )

        val refreshContext = ConcoctionRefreshContext(
            itemCount = { name ->
                when (name.lowercase()) {
                    "queue drop leaf" -> 10
                    else -> 0
                }
            },
            limitPoolsFactory = {
                ConcoctionLimitPools.forTest(
                    adventureSmithingLimit = 10,
                    adventuresUsed = ConcoctionQueueBudget.adventuresUsed,
                )
            },
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)

        fun creatableContext() = ConcoctionCreatableContext(
            initialCount = { name -> ConcoctionDatabase.initialCount(name) },
            limitPools = refreshContext.resolvedLimitPools(),
            availableCountById = refreshContext.availableCountById,
        )
        val concoction = ConcoctionDatabase.getByResult("queue drop parent")!!
        assertEquals(10, calculateCreatableTotal(concoction, creatableContext()))

        val queueContext = ConcoctionQueueContext.fromRefreshContext(refreshContext)
        ConcoctionCraftQueue.push("queue drop parent", 3, queueContext)
        assertEquals(7, calculateCreatableTotal(concoction, creatableContext()))

        ConcoctionCraftQueue.pop()
        assertEquals(10, calculateCreatableTotal(concoction, creatableContext()))
    }

    @Test
    fun refresh_pullablePass_addsStoragePullsAndQueueReducesPullable() {
        registerItems(
            8163 to "pull storage item",
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "pull storage item",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )

        ConcoctionDatabase.setPullsRemaining(5)
        ConcoctionDatabase.setPullsBudgeted(5)

        val refreshContext = ConcoctionRefreshContext(
            itemCount = { 0 },
            storageCountById = { id -> if (id == 8163) 4 else 0 },
            considerPulls = true,
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)
        assertEquals(4, ConcoctionDatabase.pullableCount("pull storage item"))
        assertEquals(4, ConcoctionDatabase.totalCount("pull storage item"))

        val queueContext = ConcoctionQueueContext.fromRefreshContext(refreshContext)
        ConcoctionCraftQueue.push("pull storage item", 3, queueContext)
        assertEquals(3, ConcoctionQueueBudget.pullsUsed)
        assertEquals(1, ConcoctionDatabase.pullableCount("pull storage item"))
        assertEquals(1, ConcoctionDatabase.totalCount("pull storage item"))

        ConcoctionCraftQueue.pop()
        assertEquals(4, ConcoctionDatabase.pullableCount("pull storage item"))
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
