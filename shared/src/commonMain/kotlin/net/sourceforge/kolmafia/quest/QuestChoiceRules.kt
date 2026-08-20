package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.adventure.RufusManager
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.CryptManager

/** Quest step bumps from choice adventure response text. */
object QuestChoiceRules {

    fun apply(
        choiceId: Int,
        responseText: String,
        questDatabase: QuestDatabase,
        decision: Int = 0,
        preferences: Preferences? = null,
        inventoryManager: InventoryManager? = null,
        optionLabel: String? = null,
        ascensionNumber: Int = 0,
        dayCount: Int = 0,
        hasCandyCaneSwordEquipped: Boolean = false,
        inPokefam: Boolean = false,
        visitHtml: String? = null,
        hasItemEquipped: (Int) -> Boolean = { false },
        itemCount: (Int) -> Int = { id ->
            inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
        },
        itemIdFromDesc: (String) -> Int? = { ItemDatabase.getByDescId(it)?.id },
        turnsPlayed: Int = 0,
        currentRun: Int = 0,
        resyncQuestLogPage1: () -> Unit = {},
        setLimitMode: (String) -> Unit = {},
        choiceUrl: String = "",
    ): Boolean {
        var advanced = false
        advanced = CandyCaneSwordSync.applyFromChoice(
            choiceId = choiceId,
            decision = decision,
            preferences = preferences,
            html = responseText,
            hasCandyCaneSwordEquipped = hasCandyCaneSwordEquipped,
        ) || advanced
        if (choiceId in 1347..1385) {
            advanced = PirateRealmSync.applyChoice(
                choiceId,
                responseText,
                decision,
                optionLabel,
                questDatabase,
                preferences,
            ) || advanced
        }
        when (choiceId) {
            142, 146 -> {
                advanced = IslandWarVisitSync.applyFromEnlistChoice(
                    decision, questDatabase, preferences, inPokefam,
                ) || advanced
            }
            523 -> {
                if (decision == 5 &&
                    responseText.contains("Your Evilometer beeps 11 times.")
                ) {
                    preferences?.let { CryptManager.decreaseEvilness(CryptManager.DEFILED_CRANNY, 11, it) }
                    advanced = true
                }
            }
            AirportNpcChoiceSync.JIMMY_CHOICE,
            AirportNpcChoiceSync.TACO_DAN_CHOICE,
            AirportNpcChoiceSync.BRODEN_CHOICE,
            -> {
                advanced = AirportNpcChoiceSync.apply(
                    choiceId = choiceId,
                    html = responseText,
                    questDatabase = questDatabase,
                    preferences = preferences,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            AirportRadioChoiceSync.CHOICE_ID -> {
                advanced = AirportRadioChoiceSync.apply(
                    html = responseText,
                    questDatabase = questDatabase,
                    preferences = preferences,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                    itemCount = { id ->
                        inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
                    },
                ) || advanced
            }
            189 -> {
                advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step26") || advanced
            }
            571, 572, 573, 576, 577 -> {
                advanced = ClancyNcSync.applyFromChoice(choiceId, questDatabase) || advanced
            }
            TwinPeakChoiceSync.ROOM_237,
            TwinPeakChoiceSync.GO_CHECK_IT_OUT,
            TwinPeakChoiceSync.HE_IS_THE_ARM,
            TwinPeakChoiceSync.NOW_ITS_DARK,
            TwinPeakChoiceSync.CABIN_FEVER,
            TwinPeakChoiceSync.NOW_ITS_DARK_ALT,
            -> {
                advanced = TwinPeakChoiceSync.apply(
                    choiceId = choiceId,
                    html = responseText,
                    preferences = preferences,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            DailyDungeonChoiceSync.FINAL_REWARD,
            DailyDungeonChoiceSync.FIRST_CHEST,
            DailyDungeonChoiceSync.SECOND_CHEST,
            DailyDungeonChoiceSync.I_WANNA_BE_A_DOOR,
            DailyDungeonChoiceSync.ALMOST_CERTAINLY_A_TRAP,
            -> {
                advanced = DailyDungeonChoiceSync.apply(
                    choiceId = choiceId,
                    html = responseText,
                    decision = decision,
                    preferences = preferences,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            TelegramChoiceSync.OFFICE,
            TelegramChoiceSync.BEGINS,
            TelegramChoiceSync.CONTINUES,
            TelegramChoiceSync.CONTINUES_AGAIN,
            TelegramChoiceSync.CONCLUDES,
            -> {
                advanced = TelegramChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    html = responseText,
                    visitHtml = visitHtml,
                    questDatabase = questDatabase,
                    preferences = preferences,
                ) || advanced
            }
            PartyFairChoiceSync.BEGINNING,
            PartyFairChoiceSync.ALL_DONE,
            PartyFairChoiceSync.PAUSED,
            PartyFairChoiceSync.ROOM_WITH_A_VIEW,
            PartyFairChoiceSync.GONE_KITCHIN,
            PartyFairChoiceSync.FORWARD_TO_THE_BACK,
            PartyFairChoiceSync.BASEMENT_URGES,
            -> {
                advanced = PartyFairChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    html = responseText,
                    questDatabase = questDatabase,
                    preferences = preferences,
                    hasItemEquipped = hasItemEquipped,
                    itemCount = itemCount,
                    itemIdFromDesc = itemIdFromDesc,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                    resyncQuestLogPage1 = resyncQuestLogPage1,
                ) || advanced
            }
            in 890..903 -> {
                advanced = LightsOutChoiceSync.apply(choiceId, responseText, preferences) || advanced
            }
            560, 561, 563, 564, 565, 566, 567, 568, 569 -> {
                advanced = WoodsDemonChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    questDatabase = questDatabase,
                    preferences = preferences,
                    itemCount = itemCount,
                ) || advanced
            }
            WalfordChoiceSync.COLLECTOR,
            WalfordChoiceSync.VYKEA,
            WalfordChoiceSync.ICE_HOTEL,
            -> {
                advanced = WalfordChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    html = responseText,
                    questDatabase = questDatabase,
                    preferences = preferences,
                ) || advanced
            }
            MeatsmithChoiceSync.HELPING_MAKE_ENDS_MEAT,
            MeatsmithChoiceSync.TEMPORARILY_OUT_OF_SKELETONS,
            -> {
                advanced = MeatsmithChoiceSync.apply(
                    choiceId = choiceId,
                    html = responseText,
                    questDatabase = questDatabase,
                    preferences = preferences,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            669, 670, 671, 675, 679 -> {
                advanced = GarbageBeanstalkSync.applyFromChoice(
                    choiceId = choiceId,
                    questDatabase = questDatabase,
                    html = responseText,
                    preferences = preferences,
                    ascensionNumber = ascensionNumber,
                    decision = decision,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            MayoMinderChoiceSync.CHOICE_ID -> {
                advanced = MayoMinderChoiceSync.apply(choiceId, decision, preferences) || advanced
            }
            MadnessBakeryChoiceSync.BAGELMAT,
            MadnessBakeryChoiceSync.ASSAULT_AND_BAGUETTERY,
            MadnessBakeryChoiceSync.POPULAR_MACHINE,
            -> {
                advanced = MadnessBakeryChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    html = responseText,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            AutopsyChoiceSync.CHOICE_ID -> {
                advanced = AutopsyChoiceSync.apply(
                    choiceId = choiceId,
                    html = responseText,
                    preferences = preferences,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            ManorTowelChoiceSync.CHOICE_ID -> {
                advanced = ManorTowelChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    html = responseText,
                    preferences = preferences,
                    ascensionNumber = ascensionNumber,
                ) || advanced
            }
            DmtChoiceSync.CHOICE_ID -> {
                advanced = DmtChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    preferences = preferences,
                    ascensionNumber = ascensionNumber,
                ) || advanced
            }
            LostKeyChoiceSync.CHOICE_ID -> {
                advanced = LostKeyChoiceSync.apply(
                    choiceId = choiceId,
                    html = responseText,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            BugbearChoiceSync.CHOICE_ID -> {
                advanced = BugbearChoiceSync.apply(
                    choiceId = choiceId,
                    html = responseText,
                    preferences = preferences,
                ) || advanced
            }
            FireStartingKitChoiceSync.CHOICE_ID -> {
                advanced = FireStartingKitChoiceSync.apply(
                    choiceId = choiceId,
                    html = responseText,
                    preferences = preferences,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            GnomePartChoiceSync.CHOICE_ID -> {
                advanced = GnomePartChoiceSync.apply(
                    choiceId = choiceId,
                    preferences = preferences,
                ) || advanced
            }
            ZombieBaitChoiceSync.CHOICE_ID -> {
                advanced = ZombieBaitChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    choiceUrl = choiceUrl,
                    itemCount = itemCount,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            SkeletonClosetChoiceSync.CHOICE_ID -> {
                advanced = SkeletonClosetChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            NumberologyChoiceSync.CHOICE_ID -> {
                advanced = NumberologyChoiceSync.apply(
                    choiceId = choiceId,
                    html = responseText,
                    preferences = preferences,
                ) || advanced
            }
            TeaTreeChoiceSync.TREE_TEA,
            TeaTreeChoiceSync.SPECIFICI_TEA,
            -> {
                advanced = TeaTreeChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    preferences = preferences,
                    choiceUrl = choiceUrl,
                ) || advanced
            }
            SpoopyChoiceSync.CHOICE_ID -> {
                advanced = SpoopyChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    html = responseText,
                    preferences = preferences,
                ) || advanced
            }
            SnojoChoiceSync.CHOICE_ID -> {
                advanced = SnojoChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    preferences = preferences,
                ) || advanced
            }
            BatfellowChoiceSync.BEGINS,
            BatfellowChoiceSync.ENDS,
            BatfellowChoiceSync.ENDS_TIMEOUT,
            -> {
                advanced = BatfellowChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    setLimitMode = setLimitMode,
                ) || advanced
            }
            BatfellowItemChoiceSync.CONSERVATORY,
            BatfellowItemChoiceSync.RESERVOIR,
            BatfellowItemChoiceSync.CEMETERY,
            BatfellowItemChoiceSync.SEWERS,
            BatfellowItemChoiceSync.ASYLUM,
            BatfellowItemChoiceSync.LIBRARY,
            BatfellowItemChoiceSync.CLOCK_FACTORY,
            BatfellowItemChoiceSync.FOUNDRY,
            BatfellowItemChoiceSync.TRIVIA_COMPANY,
            -> {
                advanced = BatfellowItemChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            OracleChoiceSync.CHOICE_ID -> {
                advanced = OracleChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    html = responseText,
                    questDatabase = questDatabase,
                    preferences = preferences,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            TimeSpinnerChoiceSync.SPINNING,
            TimeSpinnerChoiceSync.RECENT_FIGHT,
            -> {
                advanced = TimeSpinnerChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    preferences = preferences,
                    choiceUrl = choiceUrl,
                ) || advanced
            }
            GingerbreadClockChoiceSync.CHOICE_ID -> {
                advanced = GingerbreadClockChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    preferences = preferences,
                ) || advanced
            }
            SeaJellyChoiceSync.CHOICE_ID -> {
                advanced = SeaJellyChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    preferences = preferences,
                ) || advanced
            }
            LoveTunnelChoiceSync.CHOICE_ID -> {
                advanced = LoveTunnelChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    preferences = preferences,
                ) || advanced
            }
            VillainLairChoiceSync.CHOICE_PANEL,
            VillainLairChoiceSync.CHOICE_DOOR,
            VillainLairChoiceSync.CHOICE_SETTING,
            -> {
                advanced = VillainLairChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    html = responseText,
                    preferences = preferences,
                    visitHtml = visitHtml,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            DeckChoiceSync.RANDOM_CHOICE, DeckChoiceSync.CHEAT_CHOICE -> {
                advanced = DeckChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    html = responseText,
                    preferences = preferences,
                ) || advanced
            }
            in AutomatedFutureChoiceSync.CHOICE_IDS -> {
                advanced = AutomatedFutureChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    html = responseText,
                    preferences = preferences,
                ) || advanced
            }
            CrimboShrubChoiceSync.CHOICE_ID -> {
                advanced = CrimboShrubChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    preferences = preferences,
                    choiceUrl = choiceUrl,
                ) || advanced
            }
            MobiusChoiceSync.CHOICE_ID -> {
                advanced = MobiusChoiceSync.apply(
                    choiceId = choiceId,
                    html = responseText,
                    preferences = preferences,
                    turnsPlayed = turnsPlayed,
                ) || advanced
            }
            BaseballChoiceSync.CHOICE_ID -> {
                advanced = if (preferences != null) {
                    BaseballChoiceSync.apply(
                        choiceId = choiceId,
                        decision = decision,
                        html = responseText,
                        preferences = preferences,
                        currentTurn = currentRun,
                        banishMonster = BaseballChoiceSync.defaultBanish(preferences),
                        trackMonster = BaseballChoiceSync.defaultTrack(preferences),
                    )
                } else {
                    false
                } || advanced
            }
            DripHallChoiceSync.CHOICE_ID -> {
                advanced = DripHallChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    html = responseText,
                    preferences = preferences,
                    estimatedPoolSkill = preferences?.getInt("poolSkill", 0) ?: 0,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            TrickOrTreatChoiceSync.CHOICE_ID -> {
                advanced = TrickOrTreatChoiceSync.apply(
                    choiceId = choiceId,
                    preferences = preferences,
                    choiceUrl = choiceUrl,
                ) || advanced
            }
            BlechHouseChoiceSync.CHOICE_ID -> {
                advanced = BlechHouseChoiceSync.apply(
                    choiceId = choiceId,
                    preferences = preferences,
                ) || advanced
            }
            ArchSpadeChoiceSync.CHOICE_ID -> {
                advanced = ArchSpadeChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    preferences = preferences,
                ) || advanced
            }
            MonorailChoiceSync.CHOICE_ID -> {
                advanced = MonorailChoiceSync.apply(
                    choiceId = choiceId,
                    html = responseText,
                    preferences = preferences,
                    visitHtml = visitHtml,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            in VykeaChoiceSync.CHOICE_IDS -> {
                advanced = VykeaChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    html = responseText,
                    preferences = preferences,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            SpacegateVaccinatorChoiceSync.CHOICE_ID -> {
                advanced = SpacegateVaccinatorChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    html = responseText,
                    preferences = preferences,
                ) || advanced
            }
            SleazeAirportExtendedChoiceSync.YACHTZEE,
            SleazeAirportExtendedChoiceSync.BREAK_TIME,
            SleazeAirportExtendedChoiceSync.ERASER,
            -> {
                advanced = SleazeAirportExtendedChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    html = responseText,
                    questDatabase = questDatabase,
                    preferences = preferences,
                    itemCount = itemCount,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            BatfellowUpgradeChoiceSync.SUIT,
            BatfellowUpgradeChoiceSync.SEDAN,
            BatfellowUpgradeChoiceSync.CAVERN,
            -> {
                advanced = BatfellowUpgradeChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    preferences = preferences,
                ) || advanced
            }
            PalindomeSync.DRAWN_ONWARD -> {
                advanced = PalindomeSync.applyFromEdChoice(
                    html = responseText,
                    questDatabase = questDatabase,
                    itemCount = itemCount,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            DinseyKioskChoiceSync.KIOSK, DinseyKioskChoiceSync.ROLLERCOASTER, DinseyKioskChoiceSync.MAINT_MISBEHAVIN -> {
                advanced = DinseyKioskChoiceSync.apply(
                    choiceId = choiceId,
                    decision = decision,
                    html = responseText,
                    questDatabase = questDatabase,
                    preferences = preferences,
                    itemCount = itemCount,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            611 -> {
                advanced = ToppingPeakNcSync.applyFromChoice(
                    decision = decision,
                    html = responseText,
                    optionLabel = optionLabel,
                    preferences = preferences,
                ) || advanced
            }
            517 -> {
                advanced = PalindomeSync.applyFromChoice(questDatabase) || advanced
            }
            132, 929 -> {
                advanced = PyramidVisitSync.applyFromChoice(
                    choiceId = choiceId,
                    decision = decision,
                    html = responseText,
                    questDatabase = questDatabase,
                    preferences = preferences,
                    consumeItem = { itemId, qty -> inventoryManager?.consumeItemLocally(itemId, qty) },
                ) || advanced
            }
            299, 302, 303, 306, 307, 308 -> {
                advanced = SeaVisitSync.applyFromChoice(
                    choiceId = choiceId,
                    decision = decision,
                    questDatabase = questDatabase,
                    preferences = preferences,
                    turnsPlayed = turnsPlayed,
                ) || advanced
            }
            921 -> {
                advanced = setIfBetter(questDatabase, Quest.MANOR, "step1") || advanced
            }
            542 -> {
                if (responseText.contains("oddly chilly", ignoreCase = true)) {
                    advanced = setIfBetter(questDatabase, Quest.MOXIE, "step1") || advanced
                }
            }
            930 -> {
                if (responseText.contains("lucky rabbit's foot", ignoreCase = true)) {
                    advanced = setIfBetter(questDatabase, Quest.CITADEL, QuestDatabase.FINISHED) || advanced
                } else if (responseText.contains("White Citadel", ignoreCase = true) ||
                    responseText.contains("satchel", ignoreCase = true)
                ) {
                    advanced = setIfBetter(questDatabase, Quest.CITADEL, QuestDatabase.STARTED) || advanced
                }
            }
            931 -> {
                if (responseText.contains("Life Ain't Nothin But Witches and Mummies", ignoreCase = true) ||
                    responseText.contains("Witches and Mummies", ignoreCase = true)
                ) {
                    advanced = setIfBetter(questDatabase, Quest.CITADEL, "step6") || advanced
                }
            }
            932 -> {
                if (responseText.contains("No Whammies", ignoreCase = true)) {
                    advanced = setIfBetter(questDatabase, Quest.CITADEL, "step8") || advanced
                } else if (responseText.contains("steel your nerves", ignoreCase = true) ||
                    responseText.contains("White Citadel", ignoreCase = true)
                ) {
                    advanced = setIfBetter(questDatabase, Quest.CITADEL, "step9") || advanced
                }
            }
            1049 -> {
                if (responseText.contains("Epic Weapon's yours", ignoreCase = true) ||
                    responseText.contains("Epic Weapon is yours", ignoreCase = true)
                ) {
                    advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step3") || advanced
                } else if (responseText.contains("Epic Weapon", ignoreCase = true)) {
                    advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step4") || advanced
                } else if (responseText.contains("ghost", ignoreCase = true)) {
                    advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step1") || advanced
                }
            }
            1061 -> {
                if (responseText.contains("Heart of Madness", ignoreCase = true)) {
                    advanced = setIfBetter(questDatabase, Quest.ARMORER, "step1") || advanced
                }
            }
            1065 -> {
                if (decision == 1 || decision == 3) {
                    advanced = setIfBetter(questDatabase, Quest.ARMORER, QuestDatabase.STARTED) || advanced
                }
            }
            1064 -> {
                when (decision) {
                    1 -> advanced = setIfBetter(questDatabase, Quest.DOC, QuestDatabase.STARTED) || advanced
                    2 -> advanced = setIfBetter(questDatabase, Quest.DOC, QuestDatabase.FINISHED) || advanced
                }
            }
            125 -> {
                if (decision == 3) {
                    advanced = setIfBetter(questDatabase, Quest.WORSHIP, "step3") || advanced
                }
            }
            584 -> {
                if (decision == 4) {
                    advanced = setIfBetter(questDatabase, Quest.WORSHIP, "step2") || advanced
                }
            }
            1002 -> {
                if (responseText.contains("spectre nods emphatically", ignoreCase = true)) {
                    advanced = setIfBetter(questDatabase, Quest.WORSHIP, QuestDatabase.FINISHED) || advanced
                }
            }
            1087 -> {
                advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step11") || advanced
                if (responseText.contains("passed", ignoreCase = true) ||
                    responseText.contains("continue", ignoreCase = true)
                ) {
                    advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step12") || advanced
                }
            }
            1088 -> {
                advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step13") || advanced
                if (responseText.contains("BOOOOOOM", ignoreCase = true)) {
                    advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step15") || advanced
                }
            }
            1003 -> {
                advanced = ContestBoothSync.parseContestBooth(decision, responseText, preferences, questDatabase) ||
                    advanced
            }
            1005, 1008, 1011 -> {
                advanced = ContestBoothSync.parseMazeTrap(choiceId, responseText, preferences) || advanced
                advanced = ContestBoothSync.visitHedgeMazeChoice(choiceId, preferences, questDatabase) || advanced
            }
            in 1006..1012 -> {
                advanced = ContestBoothSync.visitHedgeMazeChoice(choiceId, preferences, questDatabase) || advanced
            }
            1013 -> {
                advanced = ContestBoothSync.visitHedgeMazeChoice(choiceId, preferences, questDatabase) || advanced
                advanced = setIfBetter(questDatabase, Quest.FINAL, "step5") || advanced
            }
            1015 -> {
                advanced = setIfBetter(questDatabase, Quest.FINAL, "step10") || advanced
            }
            1022 -> {
                advanced = setIfBetter(questDatabase, Quest.FINAL, "step4") || advanced
            }
            1340 -> {
                if (decision != 1) {
                    advanced = QuestSpecialSync.abandonDoctorBag(questDatabase, preferences) || advanced
                }
            }
            1341 -> {
                if (decision == 1) {
                    advanced = QuestSpecialSync.completeDoctorBagDelivery(
                        responseText,
                        questDatabase,
                        preferences,
                    ) || advanced
                }
            }
            1412 -> {
                if (decision == 1) {
                    advanced = QuestSpecialSync.abandonGuzzlr(questDatabase, preferences) || advanced
                }
            }
            1499, 1500 -> {
                preferences?.let {
                    RufusManager(it).handleShadowRiftNC(
                        choiceId = choiceId,
                        inventoryManager = inventoryManager,
                        decision = decision,
                        currentRun = currentRun,
                    )
                    advanced = true
                }
            }
            ShenSync.CHOICE_NIGHTCLUB,
            ShenSync.CHOICE_JERK,
            ShenSync.CHOICE_HUGE_JERK,
            ShenSync.CHOICE_WORLDS_BIGGEST,
            -> {
                advanced = ShenSync.applyPostChoice(
                    choiceId = choiceId,
                    html = responseText,
                    questDatabase = questDatabase,
                    preferences = preferences,
                    dayCount = dayCount,
                    consumeItem = { itemId -> inventoryManager?.consumeItemLocally(itemId, 1) },
                ) || advanced
            }
            in 780..789, 791 -> {
                advanced = HiddenCityChoiceSync.applyPostChoice(
                    choiceId = choiceId,
                    html = responseText,
                    decision = decision,
                    questDatabase = questDatabase,
                    preferences = preferences,
                    ascensionNumber = ascensionNumber,
                    consumeItem = { itemId -> inventoryManager?.consumeItemLocally(itemId, 1) },
                    itemCount = { id ->
                        inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
                    },
                ) || advanced
            }
            in 1545..1550 -> {
                advanced = CyberRealmSync.applyFromChoice(choiceId, preferences) || advanced
            }
        }
        return advanced
    }

    private fun setIfBetter(db: QuestDatabase, quest: Quest, step: String): Boolean {
        val current = db.getProgress(quest)
        if (QuestDatabase.stepOrdinal(step) > QuestDatabase.stepOrdinal(current)) {
            db.setProgress(quest, step)
            return true
        }
        return false
    }
}
