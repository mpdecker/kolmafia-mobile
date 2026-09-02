package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Central quest orchestration hub.
 *
 * Parsing remains in the focused `*Sync` objects; this class only normalizes request context and
 * dispatches the same URL and combat events as desktop QuestManager.
 */
object QuestManager {
    data class QuestChangeContext(
        val preferences: Preferences? = null,
        val questDatabase: QuestDatabase? = null,
        val characterState: CharacterState? = null,
        val character: KoLCharacter? = null,
        val inventoryManager: InventoryManager? = null,
        val gameDatabase: GameDatabase? = null,
        val sessionLogger: SessionLogger? = null,
        val adventureId: String = "",
        val locationName: String = "",
        val won: Boolean = true,
        val itemsGained: List<String> = emptyList(),
        val itemIdsGained: List<Int> = emptyList(),
        val combatItemId: Int? = null,
        val clearEquipment: (EquipmentSlot) -> Unit = {},
        val hasEffect: (Int) -> Boolean = { false },
        val adventureTurns: (String) -> Int = { 0 },
        val parseQuestLogPage: ((Int, String) -> Unit)? = null,
        val requestQuestLogPageOne: (() -> Unit)? = null,
    ) {
        fun itemCount(itemId: Int): Int =
            inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0

        fun hasItem(itemId: Int): Boolean = itemCount(itemId) > 0

        fun consumeItem(itemId: Int, quantity: Int = 1) {
            inventoryManager?.consumeItemLocally(itemId, quantity)
        }

        fun hasEquipped(itemId: Int): Boolean =
            characterState?.equipment?.values?.any { name ->
                gameDatabase?.item(name)?.id == itemId
            } == true
    }

    fun handleQuestChange(
        url: String,
        html: String,
        ctx: QuestChangeContext = QuestChangeContext(),
    ) {
        val location = url.substringAfterLast('/').substringBefore('#')
        val lower = location.lowercase()
        val state = ctx.characterState
        val prefs = ctx.preferences
        val quests = ctx.questDatabase
        val snarfblat = parseIntParameter(location, "snarfblat")

        // Redirect-only adventure handling happens before the empty-response guard.
        if (lower.startsWith("adventure.php") && html.isBlank()) {
            when (snarfblat) {
                PalindomeSync.PALINDOME_ADVENTURE ->
                    quests?.setQuestIfBetter(Quest.PALINDOME, QuestDatabase.STARTED)
                ElVibratoSync.EL_VIBRATO_ISLAND ->
                    ElVibratoSync.applyFromAdventure(snarfblat.toString(), prefs, location)
            }
            return
        }
        if (html.isBlank()) return

        when {
            lower.startsWith("adventure.php") -> routeAdventure(location, html, snarfblat, ctx)
            lower.startsWith("choice.php") && lower.contains("forceoption=0") ->
                SpacegateTerminalSync.applyFromTerminal(location, html, prefs)
            lower.startsWith("cobbsknob.php") && lower.contains("action=cell37") ->
                Cell37EscapeSync.applyFromVisit(
                    location,
                    html,
                    quests,
                    itemCount = ctx::itemCount,
                    consumeItem = ctx::consumeItem,
                )
            lower.startsWith("council") || lower.contains("action=expl_council") ->
                CouncilVisitSync.applyFromVisit(
                    location,
                    html,
                    quests,
                    prefs,
                    state?.level ?: 1,
                    ctx::consumeItem,
                )
            lower.startsWith("da.php") -> {
                if (html.contains("barrelshrine")) prefs?.setBoolean("barrelShrineUnlocked", true)
            }
            lower.startsWith("fernruin") -> FernruinVisitSync.applyFromVisit(location, quests)
            lower.startsWith("friars") ->
                FriarsQuestSync.applyCeremony(location, html, quests, prefs, ctx::consumeItem)
            lower.startsWith("main") ->
                IslandUnlockSync.applyFromMain(location, html, prefs, state?.ascensionNumber ?: 0)
            lower.startsWith("manor") -> routeManor(location, html, ctx)
            lower.startsWith("monkeycastle") ||
                lower.startsWith("sea_merkin") ||
                lower.startsWith("seafloor") -> routeSea(location, html, ctx)
            lower.startsWith("pandamonium") ->
                PandamoniumVisitSync.applyFromVisit(location, quests)
            lower.startsWith("place.php") -> routePlace(location, html, ctx)
            lower.startsWith("questlog") -> {
                val page = parseIntParameter(location, "which") ?: 1
                ctx.parseQuestLogPage?.invoke(page, html)
            }
            lower.startsWith("tavern") -> TavernVisitSync.applyFromVisit(location, html, quests)
            lower.startsWith("trickortreat") ->
                TrickOrTreatSync.applyFromVisit(
                    location,
                    html,
                    state?.equipment ?: emptyMap(),
                    clearSlot = ctx.clearEquipment,
                    consumeItem = ctx::consumeItem,
                )
            lower.startsWith("wham") -> DetectiveCaseSync.applyFromVisit(location, html, prefs)
            // generate15 is obsolete on desktop and intentionally has no mobile parser.
        }
    }

    fun handles(url: String): Boolean {
        val path = url.substringAfterLast('/').lowercase()
        return path.startsWith("adventure.php") ||
            (path.startsWith("choice.php") && path.contains("forceoption=0")) ||
            path.startsWith("cobbsknob.php") ||
            path.startsWith("council") ||
            path.startsWith("da.php") ||
            path.startsWith("fernruin") ||
            path.startsWith("friars") ||
            path.startsWith("main") ||
            path.startsWith("manor") ||
            path.startsWith("monkeycastle") ||
            path.startsWith("pandamonium") ||
            path.startsWith("place.php") ||
            path.startsWith("questlog") ||
            path.startsWith("sea_merkin") ||
            path.startsWith("seafloor") ||
            path.startsWith("tavern") ||
            path.startsWith("trickortreat") ||
            path.startsWith("wham")
    }

    private fun routeAdventure(
        url: String,
        html: String,
        snarfblat: Int?,
        ctx: QuestChangeContext,
    ) {
        val id = snarfblat?.toString()
        val prefs = ctx.preferences
        WhiteCitadelSync.applyFromAdventure(id, html, ctx.questDatabase, url)
        TowerRuinsSync.applyFromAdventure(id, html, ctx.questDatabase, url)
        ClancyNcSync.applyFromAdventure(id, html, ctx.questDatabase, url)
        ExtremeSlopeSync.applyFromAdventure(id, html, prefs, url)
        GarbageBeanstalkSync.applyFromAdventure(
            url,
            html,
            ctx.questDatabase,
            prefs,
            ctx.characterState?.ascensionNumber ?: 0,
        )
        ZeppelinRonSync.applyFromAdventure(
            url,
            html,
            ctx.questDatabase,
            prefs,
            adventureId = id,
            won = ctx.won,
        )
        PalindomeSync.applyFromVisit(
            url,
            html,
            ctx.questDatabase,
            prefs,
            PalindomeSync.PalindomeVisitContext(ctx::consumeItem),
        )
        ElVibratoSync.applyFromAdventure(id, prefs, url)
        ToppingPeakNcSync.applyFromAdventure(url, html, prefs, id)
        PirateNcSync.applyFromAdventure(id, html, ctx.questDatabase, prefs, url)
        SpookyravenManorVisitSync.applyFromVisit(url, html, ctx.questDatabase, prefs, manorContext(ctx))
        PyramidVisitSync.applyFromVisit(
            url,
            html,
            ctx.questDatabase,
            prefs,
            PyramidVisitSync.PyramidVisitContext(ctx::consumeItem),
        )
        prefs?.let { AirportSync.syncFromVisit(html, url, it) { item -> ctx.consumeItem(item) } }
        SeaVisitSync.applyFromAdventure(id, html, ctx.questDatabase, url)
        GingerbreadCitySync.applyFromVisit(url, html, prefs)
        SpacegateVisitSync.applyFromVisit(url, html, prefs)
        SpacegateAdventureSync.applyFromAdventure(url, html, prefs, id)
        FarmDuckSync.applyFromAdventure(id, html, prefs, url)
        FriarsQuestSync.applyFromAdventure(id, html, prefs, ctx.adventureTurns, url)
        CyberRealmSync.applyFromAdventure(id, html, prefs, url)
        SneakyPeteDiscardSync.applyFromAdventure(
            html,
            ctx.characterState?.inebriety ?: 0,
            ctx.characterState?.equipment ?: emptyMap(),
            clearSlot = ctx.clearEquipment,
            consumeItem = ctx::consumeItem,
        )
        if (snarfblat == BlackForestSync.BLACK_FOREST && ctx.questDatabase != null && prefs != null) {
            BlackForestSync.applyBlackForestText(ctx.questDatabase, prefs, html)
        }
    }

    private fun routePlace(url: String, html: String, ctx: QuestChangeContext) {
        val place = stringParameter(url, "whichplace")?.lowercase().orEmpty()
        val prefs = ctx.preferences
        when {
            place.startsWith("airport") ->
                prefs?.let { AirportSync.syncFromVisit(html, url, it) { item -> ctx.consumeItem(item) } }
            place == "bathole" -> BatholeSync.applyFromVisit(url, html, ctx.questDatabase)
            place == "beanstalk" -> GarbageBeanstalkSync.applyFromPlace(url, html, ctx.questDatabase)
            place == "canadia" -> SwampQuestSync.applyFromCanadia(url, html, ctx.questDatabase)
            place == "desertbeach" || place == "exploathing_beach" -> {
                // Desktop routes db_pyramid1 / expl_pyramidpre to handlePyramidChange.
                if (url.contains("action=db_pyramid1", ignoreCase = true) ||
                    url.contains("action=expl_pyramidpre", ignoreCase = true)
                ) {
                    PyramidVisitSync.applyFromVisit(
                        url,
                        html,
                        ctx.questDatabase,
                        prefs,
                        PyramidVisitSync.PyramidVisitContext(ctx::consumeItem),
                    )
                } else {
                    DesertVisitSync.applyFromVisit(url, html, ctx.questDatabase, prefs)
                }
            }
            place == "gingerbreadcity" -> GingerbreadCitySync.applyFromVisit(url, html, prefs)
            place == "hiddencity" ->
                HiddenCityVisitSync.applyFromVisit(
                    url,
                    html,
                    prefs,
                    ctx.characterState?.ascensionNumber ?: 0,
                )
            place == "serverroom" -> CyberRealmSync.applyFromServerRoom(url, html, prefs)
            place == "exploathing" && url.contains("action=expl_council", ignoreCase = true) ->
                CouncilVisitSync.applyFromVisit(
                    url,
                    html,
                    ctx.questDatabase,
                    prefs,
                    ctx.characterState?.level ?: 1,
                    ctx::consumeItem,
                )
            place.startsWith("manor") -> routeManor(url, html, ctx)
            place == "marais" ->
                prefs?.let { SwampQuestSync.applyFromMarais(url, html, ctx.questDatabase, it) }
            place == "mclargehuge" -> {
                prefs?.let {
                    ExtremeSlopeSync.applyCloudyPeak(url, html, ctx.questDatabase, it)
                    TrapperCabinSync.applyFromVisit(
                        url,
                        html,
                        ctx.questDatabase,
                        it,
                        ctx.characterState?.ascensionNumber ?: 0,
                        ctx::consumeItem,
                    )
                }
            }
            place == "monorail" -> prefs?.let {
                FantasyRealmSync.applyFromMonorail(url, html, it)
                CyberRealmSync.applyFromMonorail(url, html, it)
            }
            place == "mountains" ->
                MelvinShirtSync.applyFromVisit(url, html, ctx.questDatabase, ctx::consumeItem)
            place == "orc_chasm" || place == "highlands" -> {
                ToppingPlaceSync.applyFromChasm(
                    url,
                    html,
                    ctx.questDatabase,
                    ctx::itemCount,
                    ctx::consumeItem,
                )
                ToppingPlaceSync.applyFromHighlands(url, html, ctx.questDatabase, prefs)
            }
            place == "palindome" ->
                PalindomeSync.applyFromVisit(
                    url,
                    html,
                    ctx.questDatabase,
                    prefs,
                    PalindomeSync.PalindomeVisitContext(ctx::consumeItem),
                )
            place == "plains" ->
                PlainsVisitSync.applyFromVisit(url, html, ctx.questDatabase, ctx::consumeItem)
            place == "pyramid" ->
                PyramidVisitSync.applyFromVisit(
                    url,
                    html,
                    ctx.questDatabase,
                    prefs,
                    PyramidVisitSync.PyramidVisitContext(ctx::consumeItem),
                )
            place == "realm_fantasy" ->
                prefs?.let { FantasyRealmSync.applyFromFantasyPlace(url, html, it) }
            place == "realm_pirate" ->
                ctx.questDatabase?.let { PirateRealmSync.parseResponse(html, it, prefs) }
            place == "sea_oldman" || place == "spacegate" -> {
                if (place == "sea_oldman") routeSea(url, html, ctx)
                if (place == "spacegate") {
                    SpacegateVisitSync.applyFromVisit(url, html, prefs)
                    SpacegateTerminalSync.applyFromTerminal(url, html, prefs)
                }
            }
            place == "speakeasy" -> TownUnlockSync.applyFromSpeakeasy(url, html, prefs)
            place == "town" -> TownUnlockSync.applyFromTown(url, html, prefs)
            place == "town_right" -> TownUnlockSync.applyFromTownRight(url, html, prefs, isBadMoon(ctx))
            place == "town_wrong" -> TownUnlockSync.applyFromTownWrong(url, html, prefs, isBadMoon(ctx))
            place == "town_market" -> TownUnlockSync.applyFromTownMarket(url, html, prefs, isBadMoon(ctx))
            place == "woods" ->
                BlackForestSync.applyWoodsVisit(
                    url,
                    html,
                    ctx.questDatabase,
                    prefs,
                    ctx.characterState?.ascensionNumber ?: 0,
                ) { item -> ctx.consumeItem(item) }
            place == "zeppelin" ->
                if (html.contains("zep_mob1.gif")) {
                    ctx.questDatabase?.setQuestIfBetter(Quest.RON, "step2")
                }
        }
    }

    private fun routeManor(url: String, html: String, ctx: QuestChangeContext) {
        SpookyravenManorVisitSync.applyFromVisit(
            url,
            html,
            ctx.questDatabase,
            ctx.preferences,
            manorContext(ctx),
        )
    }

    private fun routeSea(url: String, html: String, ctx: QuestChangeContext) {
        val cls = ctx.characterState?.characterClassEnum
        SeaVisitSync.applyFromVisit(
            url,
            html,
            ctx.questDatabase,
            ctx.preferences,
            isMuscleClass = cls?.isMuscleBased == true,
            isMysticalityClass = cls?.isMysticality == true,
            isMoxieClass = cls?.isMoxieBased == true,
        )
    }

    private fun manorContext(ctx: QuestChangeContext) =
        SpookyravenManorVisitSync.ManorVisitContext(
            ascensionNumber = ctx.characterState?.ascensionNumber ?: 0,
            hasItemId = ctx::hasItem,
            consumeItem = ctx::consumeItem,
        )

    /**
     * Dispatches a completed combat through the existing quest sync source-of-truth objects.
     */
    fun updateQuestData(
        html: String,
        monsterName: String,
        ctx: QuestChangeContext = QuestChangeContext(),
    ): QuestFightRules.QuestCombatResult {
        val quests = ctx.questDatabase ?: return QuestFightRules.QuestCombatResult(false)
        val state = ctx.characterState
        val prefs = ctx.preferences
        val itemIds = if (ctx.itemIdsGained.isNotEmpty()) {
            ctx.itemIdsGained
        } else {
            ctx.itemsGained.mapNotNull { ctx.gameDatabase?.item(it)?.id }
        }
        val result = QuestFightRules.applyCombat(
            questDatabase = quests,
            monster = monsterName,
            won = ctx.won,
            itemsGained = ctx.itemsGained,
            itemIdsGained = itemIds,
            preferences = prefs,
            adventureId = ctx.adventureId,
            responseText = html,
            hasItemEquipped = ctx::hasEquipped,
            hasItemId = ctx::hasItem,
            ascensionNumber = state?.ascensionNumber ?: 0,
            combatItemId = ctx.combatItemId,
            consumeItem = ctx::consumeItem,
            currentRun = state?.currentRun ?: 0,
            character = ctx.character,
        )
        if (result.resyncQuestLogPage1) ctx.requestQuestLogPageOne?.invoke()
        result.sessionLogLines.forEach { line ->
            ctx.sessionLogger?.appendRawLine(line)
        }
        ThingWithNoNameSync.apply(
            monsterName,
            ctx.won,
            quests,
            prefs,
            state?.ascensionNumber ?: 0,
            ctx::consumeItem,
        )
        QuestItemRules.applyItemsGained(
            ctx.itemsGained,
            quests,
            ctx::hasItem,
            ctx::consumeItem,
            prefs,
            itemIds,
        )
        GuzzlrCombatSync.applyCombatWin(
            quests,
            prefs,
            ctx.locationName,
            html,
            ctx.won,
            ctx.gameDatabase,
            ctx::hasEquipped,
            ctx::itemCount,
            ctx::consumeItem,
        )
        FinalQuestCombatSync.applyCombatWin(
            quests,
            prefs,
            ctx.adventureId,
            monsterName,
            html,
            ctx.won,
        )
        ToppingPeakCombatSync.applyCombatWin(prefs, monsterName, html, ctx.won, ctx::hasEquipped)
        val warEnded = IslandWarCombatSync.applyEndOfWar(
            quests,
            prefs,
            ctx.adventureId,
            monsterName,
            html,
            ctx.won,
            state?.isKingdomOfExploathing == true,
            ctx.sessionLogger,
        )
        if (!warEnded) {
            IslandWarCombatSync.applyCombatWin(
                prefs,
                ctx.adventureId,
                html,
                ctx.won,
                monsterName,
                state?.isKingdomOfExploathing == true,
                ctx.sessionLogger,
            )
        }
        IslandWarCombatSync.applyNunsSidequestWin(prefs, monsterName, html, ctx.won)
        SpookyravenCombatSync.applyCombatWin(quests, prefs, monsterName, ctx.won, ctx::hasItem)
        PyramidCombatSync.applyChamberProgress(quests, prefs, ctx.adventureId, html)
        PalindomeSync.applyCombatWin(quests, prefs, monsterName, ctx.won)
        DesertCombatSync.applyCombatWin(
            quests,
            prefs,
            ctx.adventureId,
            html,
            ctx.won,
            DesertCombatSync.DesertCombatContext(ctx::hasEquipped, ctx.hasEffect, state?.familiarId ?: 0),
        )
        BlackForestSync.applyCombatWin(quests, prefs, ctx.adventureId, html, ctx.won)
        FantasyRealmCombatSync.applyCombatWin(monsterName, ctx.adventureId, prefs, ctx.won)
        HiddenCityCombatSync.applyCombatWin(
            quests,
            prefs,
            ctx.adventureId,
            monsterName,
            html,
            ctx.won,
            state?.ascensionNumber ?: 0,
            ctx::itemCount,
        )
        if (ctx.adventureId.isNotBlank()) {
            routeAdventure(
                "adventure.php?snarfblat=${ctx.adventureId}",
                html,
                ctx.adventureId.toIntOrNull(),
                ctx,
            )
        }
        LatteChoiceSync.applyFight(ctx.locationName, html, prefs)
        ProtonicGhostSync.applyFromFight(
            html,
            quests,
            prefs,
            state?.turnsPlayed ?: 0,
            state?.equipment ?: emptyMap(),
        )
        return result
    }

    fun fightStarted(
        html: String,
        monsterName: String,
        ctx: QuestChangeContext = QuestChangeContext(),
    ): Boolean {
        var changed = ctx.questDatabase?.let { QuestFightRules.applyFightStarted(it, monsterName) } ?: false
        changed = QuestFightStartedSync.apply(
            monsterName,
            html,
            ctx.preferences,
            ctx.characterState?.turnsPlayed ?: 0,
            ctx.characterState?.equipment ?: emptyMap(),
            clearSlot = ctx.clearEquipment,
            consumeItem = ctx::consumeItem,
        ) || changed
        return changed
    }

    fun fightLost(
        html: String,
        monsterName: String,
        ctx: QuestChangeContext = QuestChangeContext(),
    ): Boolean = QuestFightLostSync.apply(monsterName, html, ctx.questDatabase, ctx.preferences)

    fun itemUsed(
        itemId: Int,
        html: String,
        ctx: QuestChangeContext = QuestChangeContext(),
        count: Int = 1,
    ): Boolean {
        updateCyrusAdjective(itemId, ctx.preferences)
        return QuestItemUsedSync.apply(
            itemId,
            html,
            ctx.questDatabase,
            ctx.preferences,
            ctx::consumeItem,
            count,
        )
    }

    fun itemEquipped(
        itemId: Int,
        ctx: QuestChangeContext = QuestChangeContext(),
    ): Boolean = QuestItemEquippedSync.apply(itemId, ctx.questDatabase)

    fun updateCyrusAdjective(itemId: Int, preferences: Preferences?): Boolean {
        val adjective = when (itemId) {
            QuestItemUsedSync.CA_BASE_PAIR -> "stronger"
            QuestItemUsedSync.CG_BASE_PAIR -> "smarter"
            QuestItemUsedSync.CT_BASE_PAIR -> "more attractive"
            QuestItemUsedSync.AG_BASE_PAIR -> "faster"
            QuestItemUsedSync.AT_BASE_PAIR -> "more aggressive"
            QuestItemUsedSync.GT_BASE_PAIR -> "more resilient"
            else -> return false
        }
        return updateCyrusAdjective(adjective, preferences)
    }

    fun updateCyrusAdjective(itemId: Int, ctx: QuestChangeContext): Boolean =
        updateCyrusAdjective(itemId, ctx.preferences)

    fun updateCyrusAdjective(adjective: String, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return false
        val before = prefs.getString("cyrusAdjectives", "")
        QuestSpecialSync.appendCyrusAdjective(prefs, adjective)
        return before != prefs.getString("cyrusAdjectives", "")
    }

    private fun isBadMoon(ctx: QuestChangeContext): Boolean =
        ctx.characterState?.zodiacSign?.contains("bad moon", ignoreCase = true) == true

    private fun parseIntParameter(url: String, name: String): Int? =
        stringParameter(url, name)?.toIntOrNull()

    private fun stringParameter(url: String, name: String): String? =
        Regex("""(?:[?&]|^)$name=([^&#]+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1)
}

typealias QuestChangeContext = QuestManager.QuestChangeContext
