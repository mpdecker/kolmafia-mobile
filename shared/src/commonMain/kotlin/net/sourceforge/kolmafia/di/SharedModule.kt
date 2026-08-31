package net.sourceforge.kolmafia.di

import io.ktor.client.*
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.adventure.AdventureRequest
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.adventure.FightRequest
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry
import net.sourceforge.kolmafia.adventure.choice.ChoiceSolvers
import net.sourceforge.kolmafia.adventure.choice.handlers.ComplexHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.DreadsylvaniaHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.GoalHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.HiddenCityHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.InventoryHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.MiscHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.QuestHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.ResponseTextHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.SkillUsesHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.SolverHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.StatHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.VillainLairHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.RufusHandlers
import net.sourceforge.kolmafia.adventure.RufusManager
import net.sourceforge.kolmafia.adventure.choice.solvers.LightsOutSolverImpl
import net.sourceforge.kolmafia.adventure.choice.solvers.SafetyShelterSolverImpl
import net.sourceforge.kolmafia.adventure.choice.solvers.LostKeySolverImpl
import net.sourceforge.kolmafia.adventure.choice.solvers.ArcadeGameSolverImpl
import net.sourceforge.kolmafia.adventure.choice.solvers.GameproSolverImpl
import net.sourceforge.kolmafia.adventure.choice.solvers.VampOutSolverImpl
import net.sourceforge.kolmafia.maximizer.MaximizerManager
import net.sourceforge.kolmafia.servant.EdServantManager
import net.sourceforge.kolmafia.vykea.VykeaCompanionManager
import net.sourceforge.kolmafia.thrall.PastaThrallManager
import net.sourceforge.kolmafia.session.BreakfastManager
import net.sourceforge.kolmafia.session.AdventureSpentTracker
import net.sourceforge.kolmafia.session.DreadKissesTracker
import net.sourceforge.kolmafia.session.DemonInCombatNameSync
import net.sourceforge.kolmafia.session.DemonNamesManager
import net.sourceforge.kolmafia.session.IntergnatDemonNameSync
import net.sourceforge.kolmafia.session.YegDemonNameSync
import net.sourceforge.kolmafia.request.CargoCultistShortsRequest
import net.sourceforge.kolmafia.session.ConcoctionQueueRunner
import net.sourceforge.kolmafia.session.CleanupJunkRunner
import net.sourceforge.kolmafia.session.AutoMallRunner
import net.sourceforge.kolmafia.session.QuarkRunner
import net.sourceforge.kolmafia.session.CargoCultManager
import net.sourceforge.kolmafia.session.CargoPocketSync
import net.sourceforge.kolmafia.session.AlliedRadioManager
import net.sourceforge.kolmafia.session.SummoningChamberManager
import net.sourceforge.kolmafia.request.AlliedRadioRequest
import net.sourceforge.kolmafia.request.SummoningChamberRequest
import net.sourceforge.kolmafia.session.WildfireCampManager
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.character.DailyResourceTracker
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.request.QuestLogRequest
import net.sourceforge.kolmafia.session.GoalManager
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.ash.ScriptHookRunner
import net.sourceforge.kolmafia.ash.ScriptManager
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarRequest
import net.sourceforge.kolmafia.http.createKoLHttpClient
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.JunkListManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.CafePurchaseRequest
import net.sourceforge.kolmafia.request.CrimboCafeRequest
import net.sourceforge.kolmafia.request.FloundryRequest
import net.sourceforge.kolmafia.request.CafeRequest
import net.sourceforge.kolmafia.request.ChezSnooteeRequest
import net.sourceforge.kolmafia.request.ClipArtCreateRequest
import net.sourceforge.kolmafia.request.ConcoctionCreateRequest
import net.sourceforge.kolmafia.request.HellKitchenRequest
import net.sourceforge.kolmafia.request.MicroBreweryRequest
import net.sourceforge.kolmafia.request.RollingPinCreateRequest
import net.sourceforge.kolmafia.request.FalloutShelterRequest
import net.sourceforge.kolmafia.request.VykeaCreateRequest
import net.sourceforge.kolmafia.request.MuseCreateRequest
import net.sourceforge.kolmafia.request.PhineasCreateRequest
import net.sourceforge.kolmafia.request.BarrelCreateRequest
import net.sourceforge.kolmafia.request.JewelCreateRequest
import net.sourceforge.kolmafia.request.MalusCreateRequest
import net.sourceforge.kolmafia.request.ModeableRequest
import net.sourceforge.kolmafia.request.FoldItemRequest
import net.sourceforge.kolmafia.request.HorseryRequest
import net.sourceforge.kolmafia.request.BoomBoxRequest
import net.sourceforge.kolmafia.request.AbsorbRequest
import net.sourceforge.kolmafia.request.MindControlRequest
import net.sourceforge.kolmafia.request.StaffCreateRequest
import net.sourceforge.kolmafia.request.GnomeTinkerCreateRequest
import net.sourceforge.kolmafia.request.SushiCreateRequest
import net.sourceforge.kolmafia.request.SewerCreateRequest
import net.sourceforge.kolmafia.request.TerminalExtrudeCreateRequest
import net.sourceforge.kolmafia.request.TerminalRequest
import net.sourceforge.kolmafia.request.ChoiceUseCreateRequest
import net.sourceforge.kolmafia.request.SausageOMaticCreateRequest
import net.sourceforge.kolmafia.request.BurningLeavesCreateRequest
import net.sourceforge.kolmafia.request.FloundryCreateRequest
import net.sourceforge.kolmafia.request.StillSuitCreateRequest
import net.sourceforge.kolmafia.request.MayamCreateRequest
import net.sourceforge.kolmafia.request.MayamRequest
import net.sourceforge.kolmafia.request.PhotoBoothCreateRequest
import net.sourceforge.kolmafia.request.PhotoBoothRequest
import net.sourceforge.kolmafia.request.TakerSpaceCreateRequest
import net.sourceforge.kolmafia.request.GnomePartCreateRequest
import net.sourceforge.kolmafia.request.SpacegateCreateRequest
import net.sourceforge.kolmafia.request.FantasyRealmCreateRequest
import net.sourceforge.kolmafia.request.ClanRumpusRequest
import net.sourceforge.kolmafia.request.ClanMembersRequest
import net.sourceforge.kolmafia.request.ClanLogRequest
import net.sourceforge.kolmafia.request.ClanWarRequest
import net.sourceforge.kolmafia.request.ClanBuffRequest
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.request.PlaceRequest
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.LoginRequest
import net.sourceforge.kolmafia.request.ManageStoreRequest
import net.sourceforge.kolmafia.request.OceanRequest
import net.sourceforge.kolmafia.request.ResearchBenchRequest
import net.sourceforge.kolmafia.request.GourdRequest
import net.sourceforge.kolmafia.session.SessionManager
import net.sourceforge.kolmafia.buffbot.BuffBotDatabase
import net.sourceforge.kolmafia.buffbot.BuffBotManager
import net.sourceforge.kolmafia.faxbot.FaxBotDatabase
import net.sourceforge.kolmafia.faxbot.FaxBotManager
import net.sourceforge.kolmafia.chat.ChatManager
import net.sourceforge.kolmafia.chat.ChatPoller
import net.sourceforge.kolmafia.chat.ChatProbe
import net.sourceforge.kolmafia.chat.ChatSender
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPriceManager
import net.sourceforge.kolmafia.mall.MallPurchaseRequest
import net.sourceforge.kolmafia.mall.MallSearchRequest
import net.sourceforge.kolmafia.npc.NpcBuyRequest
import net.sourceforge.kolmafia.request.AutosellRequest
import net.sourceforge.kolmafia.request.AutoMallRequest
import net.sourceforge.kolmafia.request.BasementRequest
import net.sourceforge.kolmafia.request.PulverizeRequest
import net.sourceforge.kolmafia.request.UntinkerRequest
import net.sourceforge.kolmafia.request.ZapRequest
import net.sourceforge.kolmafia.request.ChewRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.request.CustomOutfitRequest
import net.sourceforge.kolmafia.request.CraftRequest
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.session.EquipmentManager
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.request.UseItemConsumptionSync
import net.sourceforge.kolmafia.request.DrinkBoozeRequest
import net.sourceforge.kolmafia.request.EatFoodRequest
import net.sourceforge.kolmafia.request.SendGiftRequest
import net.sourceforge.kolmafia.request.SendMailRequest
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.request.HermitRequest
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.request.StillSuitRequest
import net.sourceforge.kolmafia.request.ActionBarRequest
import net.sourceforge.kolmafia.request.TrendyRequest
import net.sourceforge.kolmafia.request.ThriftyRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.request.HashingViseRequest
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.shop.CoinmasterManager
import net.sourceforge.kolmafia.shop.CoinmasterRequest
import net.sourceforge.kolmafia.shop.ShopRequest
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.mood.ManaBurnManager
import net.sourceforge.kolmafia.mood.MoodManager
import net.sourceforge.kolmafia.request.UneffectRequest
import net.sourceforge.kolmafia.recovery.RecoveryManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val sharedModule = module {
    single { GameDatabase() }
    single { DailyResourceTracker() }
    single<HttpClient> { createKoLHttpClient() }
    single { KoLCharacter() }
    single { Preferences(get()) }
    single { GameEventBus() }
    singleOf(::LoginRequest)
    singleOf(::CharacterRequest)
    single {
        AdventureRequest(
            client = get(),
            preferences = get(),
            effectManager = get(),
            questDatabase = get(),
        )
    }
    singleOf(::FightRequest)
    singleOf(::ChoiceRequest)
    singleOf(::OceanRequest)
    singleOf(::ResearchBenchRequest)
    single {
        GourdRequest(
            client = get(),
            preferences = get(),
            character = get(),
            inventory = get(),
            sessionLogger = get(),
        )
    }
    single { GoalManager() }
    single { QuestDatabase(get()) }
    single { QuestLogRequest(get(), get(), get(), get()) }
    single {
        ChoiceSolvers(
            safetyShelter = SafetyShelterSolverImpl(),
            vampOut       = VampOutSolverImpl(get()),
            arcadeGame    = ArcadeGameSolverImpl(),
            lostKey       = LostKeySolverImpl(),
            gamepro       = GameproSolverImpl(get()),
            lightsOut     = LightsOutSolverImpl(get()),
        )
    }
    singleOf(::RufusManager)
    single {
        ChoiceHandlerRegistry().also { r ->
            InventoryHandlers.registerAll(r)
            ResponseTextHandlers.registerAll(r)
            StatHandlers.registerAll(r)
            ComplexHandlers.registerAll(r)
            DreadsylvaniaHandlers.registerAll(r)
            HiddenCityHandlers.registerAll(r)
            MiscHandlers.registerAll(r)
            GoalHandlers.registerAll(r)
            QuestHandlers.registerAll(r)
            SkillUsesHandlers.registerAll(r)
            SolverHandlers.registerAll(r)
            VillainLairHandlers.registerAll(r)
            RufusHandlers.registerAll(r, get())
        }
    }
    single {
        UseItemRequest(
            client = get(),
            preferences = get(),
            sessionLogger = get(),
            eventBus = get(),
            questDatabase = get(),
            character = get(),
            inventoryManager = get(),
        )
    }
    single {
        HashingViseRequest(
            client = get(),
            choiceRequest = get(),
            inventoryManager = get(),
            preferences = get(),
            sessionLogger = get(),
        )
    }
    single {
        FoldItemRequest(
            client = get(),
            useItemRequest = get(),
            choiceRequest = get(),
            equipmentRequest = get(),
            inventoryManager = get(),
            retrieveItemService = get(),
            recoveryManager = get(),
            character = get(),
            skillManager = get(),
            preferences = get(),
            gameDatabase = get(),
        )
    }
    singleOf(::HermitRequest)
    singleOf(::ThriftyRequest)
    singleOf(::StandardRequest)
    singleOf(::TrendyRequest)
    single {
        EatFoodRequest(
            client = get(),
            preferences = get(),
            character = get(),
            inventoryManager = get(),
            sessionLogger = get(),
        )
    }
    single {
        DrinkBoozeRequest(
            client = get(),
            preferences = get(),
            character = get(),
            inventoryManager = get(),
            sessionLogger = get(),
        )
    }
    single {
        ChewRequest(
            client = get(),
            preferences = get(),
            character = get(),
            inventoryManager = get(),
            sessionLogger = get(),
        )
    }
    singleOf(::AutosellRequest)
    singleOf(::AutoMallRequest)
    singleOf(::BasementRequest)
    single { PulverizeRequest(get(), get(), get(), get(), get()) }
    single { JunkListManager(get()) }
    single {
        CleanupJunkRunner(
            junkListManager = get(),
            inventoryManager = get(),
            untinkerRequest = get(),
            pulverizeRequest = get(),
            useItemRequest = get(),
            autosellRequest = get(),
            skillManager = get(),
            character = get(),
            gameDatabase = get(),
            closetRequest = get(),
        )
    }
    single {
        AutoMallRunner(
            junkListManager = get(),
            inventoryManager = get(),
            manageStoreRequest = get(),
            character = get(),
            gameDatabase = get(),
            autoMallRequest = get(),
        )
    }
    single {
        QuarkRunner(
            junkListManager = get(),
            inventoryManager = get(),
            craftRequest = get(),
            retrieveItemService = get(),
            character = get(),
            gameDatabase = get(),
        )
    }
    single {
        UntinkerRequest(
            client = get(),
            inventoryManager = get(),
            retrieveItemService = get(),
            gameDatabase = get(),
            character = get(),
            adventureManager = get(),
            goalManager = get(),
            questDatabase = get(),
        )
    }
    single {
        ZapRequest(
            client = get(),
            inventoryManager = get(),
            retrieveItemService = get(),
            preferences = get(),
            character = get(),
            useItemRequest = get(),
        )
    }
    singleOf(::ClosetRequest)
    singleOf(::StorageRequest)
    singleOf(::DisplayCaseRequest)
    singleOf(::ClanStashRequest)
    singleOf(::SendMailRequest)
    singleOf(::SendGiftRequest)
    singleOf(::ManageStoreRequest)
    single {
        EquipmentManager(
            character = get(),
            inventoryManager = get(),
            skillManager = get(),
        ).also { em ->
            ResultProcessor.equipmentManagerProvider = { em }
            ResultProcessor.hasEquipped = { em.hasEquipped(it) }
            UseItemConsumptionSync.equipmentManagerProvider = { em }
        }
    }
    single {
        EquipmentRequest(
            client = get(),
            characterRequest = get(),
            character = get(),
            questDatabase = get(),
            equipmentManager = get(),
        )
    }
    singleOf(::CustomOutfitRequest)
    single {
        CampgroundRequest(
            client = get(),
            preferences = get(),
            character = get(),
            inventoryManager = get(),
            sessionLogger = get(),
        )
    }
    single {
        PlaceRequest(
            client = get(),
            preferences = get(),
            character = get(),
            inventoryManager = get(),
            sessionLogger = get(),
        )
    }
    singleOf(::FalloutShelterRequest)
    single {
        TerminalRequest(
            client = get(),
            campgroundRequest = get(),
            falloutShelterRequest = get(),
        )
    }
    singleOf(::ClanRumpusRequest)
    singleOf(::ClanLoungeRequest)
    singleOf(::ClanMembersRequest)
    singleOf(::ClanLogRequest)
    singleOf(::ClanWarRequest)
    singleOf(::ClanBuffRequest)
    singleOf(::CafeRequest)
    single {
        HellKitchenRequest(cafeRequest = get())
    }
    single {
        ChezSnooteeRequest(hellKitchenRequest = get())
    }
    single {
        MicroBreweryRequest(hellKitchenRequest = get())
    }
    single {
        CrimboCafeRequest(cafeRequest = get())
    }
    single {
        CafePurchaseRequest(
            hellKitchenRequest = get(),
            chezSnooteeRequest = get(),
            microBreweryRequest = get(),
            crimboCafeRequest = get(),
        )
    }
    single {
        ConcoctionCreateRequest(
            retrieveItemService = get(),
            craftRequest = get(),
            useItemRequest = get(),
            gameDatabase = get(),
            createItemIngredients = get(),
            shopRequest = get(),
            coinmasterManager = get(),
            character = get(),
            preferences = get(),
            inventoryManager = get(),
            clipArtCreateRequest = ClipArtCreateRequest(get()),
            rollingPinCreateRequest = RollingPinCreateRequest(
                useItemRequest = get(),
                retrieveItemService = get(),
                gameDatabase = get(),
                npcBuyRequest = get(),
                preferences = get(),
                inventoryManager = get(),
                character = get(),
            ),
            terminalExtrudeCreateRequest = TerminalExtrudeCreateRequest(
                terminalRequest = get(),
                createItemIngredients = get(),
            ),
            sewerCreateRequest = SewerCreateRequest(
                useItemRequest = get(),
                closetRequest = get(),
                createItemIngredients = get(),
                gameDatabase = get(),
                inventoryCountById = { id ->
                    get<InventoryManager>().state.value.items[id]?.quantity ?: 0
                },
            ),
            vykeaCreateRequest = VykeaCreateRequest(
                useItemRequest = get(),
                choiceRequest = get(),
                retrieveItemService = get(),
                createItemIngredients = get(),
                vykeaCompanionManager = get(),
                gameDatabase = get(),
                accessibleCount = { id ->
                    get<InventoryManager>().state.value.items[id]?.quantity ?: 0
                },
            ),
            museCreateRequest = MuseCreateRequest(
                useItemRequest = get(),
                createItemIngredients = get(),
                gameDatabase = get(),
            ),
            phineasCreateRequest = PhineasCreateRequest(
                client = get(),
                createItemIngredients = get(),
                gameDatabase = get(),
            ),
            staffCreateRequest = StaffCreateRequest(
                client = get(),
                createItemIngredients = get(),
                gameDatabase = get(),
                sessionLogger = get(),
                eventBus = get(),
            ),
            gnomeTinkerCreateRequest = GnomeTinkerCreateRequest(
                client = get(),
                createItemIngredients = get(),
                gameDatabase = get(),
            ),
            sushiCreateRequest = SushiCreateRequest(
                client = get(),
                createItemIngredients = get(),
                gameDatabase = get(),
                inventoryManager = get(),
                character = get(),
                sessionLogger = get(),
                preferences = get(),
                eventBus = get(),
            ),
            malusCreateRequest = MalusCreateRequest(
                client = get(),
                createItemIngredients = get(),
                gameDatabase = get(),
                skillManager = get(),
                sessionLogger = get(),
                eventBus = get(),
            ),
            jewelCreateRequest = JewelCreateRequest(
                craftRequest = get(),
                createItemIngredients = get(),
                gameDatabase = get(),
                accessibleCount = { id ->
                    get<InventoryManager>().state.value.items[id]?.quantity ?: 0
                },
            ),
            barrelCreateRequest = BarrelCreateRequest(
                client = get(),
                choiceRequest = get(),
                preferences = get(),
            ),
            waxCreateRequest = ChoiceUseCreateRequest(
                useItemRequest = get(),
                choiceRequest = get(),
                createItemIngredients = get(),
                gameDatabase = get(),
                sourceItemId = ChoiceUseCreateRequest.WAX_GLOB,
                choiceId = ChoiceUseCreateRequest.WAX_CHOICE,
                itemIdToOption = ChoiceUseCreateRequest::waxOption,
                exitOption = 6,
            ),
            newspaperCreateRequest = ChoiceUseCreateRequest(
                useItemRequest = get(),
                choiceRequest = get(),
                createItemIngredients = get(),
                gameDatabase = get(),
                sourceItemId = ChoiceUseCreateRequest.BURNING_NEWSPAPER,
                choiceId = ChoiceUseCreateRequest.NEWSPAPER_CHOICE,
                itemIdToOption = ChoiceUseCreateRequest::newspaperOption,
            ),
            meteoroidCreateRequest = ChoiceUseCreateRequest(
                useItemRequest = get(),
                choiceRequest = get(),
                createItemIngredients = get(),
                gameDatabase = get(),
                sourceItemId = ChoiceUseCreateRequest.METAL_METEOROID,
                choiceId = ChoiceUseCreateRequest.METEOROID_CHOICE,
                itemIdToOption = ChoiceUseCreateRequest::meteoroidOption,
            ),
            woolCreateRequest = ChoiceUseCreateRequest(
                useItemRequest = get(),
                choiceRequest = get(),
                createItemIngredients = get(),
                gameDatabase = get(),
                sourceItemId = ChoiceUseCreateRequest.GRUBBY_WOOL,
                choiceId = ChoiceUseCreateRequest.WOOL_CHOICE,
                itemIdToOption = ChoiceUseCreateRequest::woolOption,
            ),
            sausageCreateRequest = SausageOMaticCreateRequest(
                client = get(),
                createItemIngredients = get(),
                retrieveItemService = get(),
                gameDatabase = get(),
                preferences = get(),
                inventoryManager = get(),
            ),
            burningLeavesCreateRequest = BurningLeavesCreateRequest(
                client = get(),
                choiceRequest = get(),
                createItemIngredients = get(),
                gameDatabase = get(),
                preferences = get(),
            ),
            floundryCreateRequest = FloundryCreateRequest(
                floundryRequest = get(),
                gameDatabase = get(),
            ),
            stillSuitCreateRequest = StillSuitCreateRequest(stillSuitRequest = get()),
            mayamCreateRequest = MayamCreateRequest(
                mayamRequest = get(),
                inventoryCount = { id -> get<InventoryManager>().state.value.items[id]?.quantity ?: 0 },
            ),
            photoBoothCreateRequest = PhotoBoothCreateRequest(
                client = get(),
                choiceRequest = get(),
            ),
            takerSpaceCreateRequest = TakerSpaceCreateRequest(
                client = get(),
                choiceRequest = get(),
                preferences = get(),
            ),
            gnomePartCreateRequest = GnomePartCreateRequest(
                client = get(),
                choiceRequest = get(),
                preferences = get(),
            ),
            spacegateCreateRequest = SpacegateCreateRequest(
                client = get(),
                choiceRequest = get(),
            ),
            fantasyRealmCreateRequest = FantasyRealmCreateRequest(
                client = get(),
                choiceRequest = get(),
            ),
        )
    }
    single {
        StillSuitRequest(
            client = get(),
            inventoryManager = get(),
        )
    }
    singleOf(::ActionBarRequest)
    singleOf(::FloundryRequest)
    single {
        MayamRequest(
            useItemRequest = get(),
            choiceRequest = get(),
        )
    }
    single {
        PhotoBoothRequest(
            client = get(),
            choiceRequest = get(),
        )
    }
    single {
        ConcoctionQueueRunner(
            clanLoungeRequest = get(),
            eatFoodRequest = get(),
            drinkBoozeRequest = get(),
            chewRequest = get(),
            useItemRequest = get(),
            retrieveItemService = get(),
            concoctionCreateRequest = get(),
            cafePurchaseRequest = get(),
            stillSuitRequest = get(),
            floundryRequest = get(),
            familiarManager = get(),
        )
    }
    single {
        FamiliarManager(
            client = get(),
            eventBus = get(),
            preferences = get(),
        )
    }
    single {
        FamiliarRequest(
            client = get(),
            familiarManager = get(),
            preferences = get(),
            character = get(),
            equipmentManager = get(),
            sessionLogger = get(),
        )
    }
    single {
        ModeableRequest(
            client = get(),
            choiceRequest = get(),
            equipmentRequest = get(),
            character = get(),
            preferences = get(),
        )
    }
    single {
        HorseryRequest(
            client = get(),
            choiceRequest = get(),
            preferences = get(),
        )
    }
    single {
        BoomBoxRequest(
            useItemRequest = get(),
            preferences = get(),
        )
    }
    single {
        AbsorbRequest(
            client = get(),
            character = get(),
            gameDatabase = get(),
            retrieveItemService = get(),
        )
    }
    single {
        MindControlRequest(
            client = get(),
            character = get(),
            preferences = get(),
            retrieveItemService = get(),
        )
    }
    single {
        MaximizerManager(
            gameDatabase = get(),
            inventoryManager = get(),
            equipmentRequest = get(),
            character = get(),
            closetRequest = get(),
            storageRequest = get(),
            displayCaseRequest = get(),
            clanStashRequest = get(),
            familiarManager = get(),
            preferences = get(),
            standardRequest = get(),
            thriftyRequest = get(),
            trendyRequest = get(),
            skillManager = get(),
            retrieveItemService = get(),
            mallPriceManager = get(),
            mallManager = get(),
            modeableRequest = get(),
            effectManager = get(),
            characterRequest = get(),
            foldItemRequest = get(),
        )
    }
    singleOf(::SessionLogger)
    single {
        BreakfastManager(
            campgroundRequest = get(),
            clanRumpusRequest = get(),
            clanLoungeRequest = get(),
            preferences = get(),
            useItemRequest = get(),
            hermitRequest = get(),
            httpClient = get(),
            familiarManager = get(),
            questDatabase = get(),
            outfitManager = get(),
            inventoryManager = get(),
            skillManager = get(),
        )
    }
    single {
        InventoryManager(
            client = get(),
            eventBus = get(),
            characterRequest = get(),
            character = get(),
            preferences = get(),
        )
    }
    single {
        SkillCastRequest(
            client = get(),
            preferences = get(),
            character = get(),
            inventoryManager = get(),
            equipmentManager = get(),
            equipmentRequest = get(),
            sessionLogger = get(),
            effectManager = get(),
        )
    }
    single {
        SkillManager(
            client = get(),
            castRequest = get(),
            eventBus = get(),
            preferences = get(),
            sessionLogger = get(),
        )
    }
    singleOf(::RecoveryManager)
    single {
        val retrieve: RetrieveItemService = get()
        UneffectRequest(
            client = get(),
            effectManager = get(),
            inventoryManager = get(),
            preferences = get(),
            sessionLogger = get(),
            retrieveItem = { itemId -> retrieve.retrieve(itemId, 1) > 0 },
            passwordHash = { get<Preferences>().getString("pwdHash", "") },
        )
    }
    single { MoodManager(skillManager = get(), preferences = get(), uneffectRequest = get()) }
    singleOf(::ManaBurnManager)
    singleOf(::BanishManager)
    singleOf(::AdventureSpentTracker)
    singleOf(::DreadKissesTracker)
    singleOf(::IntergnatDemonNameSync)
    singleOf(::YegDemonNameSync)
    single { CargoPocketSync(get(), get()) }
    single {
        CargoCultManager(
            preferences = get(),
            request = CargoCultistShortsRequest(get()),
            pocketSync = get(),
            yegDemonNameSync = get(),
            inventoryManager = get(),
        )
    }
    singleOf(::DemonInCombatNameSync)
    single {
        DemonNamesManager(
            preferences = get(),
            segmentSync = get(),
        )
    }
    single {
        SummoningChamberManager(
            preferences = get(),
            request = SummoningChamberRequest(get()),
            retrieveItemService = get(),
            inventoryManager = get(),
            familiarRequest = get(),
            familiarManager = get(),
        )
    }
    single {
        AlliedRadioManager(
            preferences = get(),
            request = AlliedRadioRequest(get(), get()),
            inventoryManager = get(),
            segmentSync = get(),
        )
    }
    singleOf(::WildfireCampManager)
    single {
        EffectManager(get(), get()).also { manager ->
            UseItemConsumptionSync.effectManagerProvider = { manager }
        }
    }
    single {
        EdServantManager(
            httpClient = get(),
            preferences = get(),
            character = get(),
        )
    }
    single {
        VykeaCompanionManager(preferences = get())
    }
    single {
        PastaThrallManager(
            preferences = get(),
            character = get(),
        )
    }
    single {
        AdventureManager(
            adventureRequest = get(),
            fightRequest     = get(),
            choiceRequest    = get(),
            characterRequest = get(),
            character        = get(),
            preferences      = get(),
            eventBus         = get(),
            registry         = get(),
            goalManager      = get(),
            questDatabase    = get(),
            solvers          = get(),
            inventory        = get(),
            effects          = get(),
            skills           = get(),
            recoveryManager  = get(),
            moodManager      = get(),
            questLogRequest  = get(),
            manaBurnManager  = get(),
            banishManager    = get(),
            gameDatabase     = get(),
            outfitManager    = get(),
            retrieveItemService = get(),
            useItemRequest   = get(),
            familiarManager  = get(),
            scriptHookRunner = get(),
            combatMacroResolver = { zoneId -> get<GameRuntimeLibrary>().resolveCombatMacro(zoneId) },
            edServantManager = get(),
            adventureSpentTracker = get(),
            dreadKissesTracker = get(),
            intergnatDemonNameSync = get(),
            yegDemonNameSync = get(),
            cargoPocketSync = get(),
            demonInCombatNameSync = get(),
            sessionLogger = get(),
            oceanRequest = get(),
            equipmentRequest = get(),
        )
    }
    single {
        GameRuntimeLibrary(
            character        = get(),
            inventoryManager = get(),
            skillManager     = get(),
            effectManager    = get(),
            adventureManager = get(),
            familiarManager  = get(),
            goalManager      = get(),
            moodManager      = get(),
            manaBurnManager  = get(),
            preferences      = get(),
            gameDatabase     = get(),
            useItemRequest   = get(),
            eatFoodRequest   = get(),
            drinkBoozeRequest = get(),
            chewRequest      = get(),
            cafePurchaseRequest = get(),
            stillSuitRequest = get(),
            actionBarRequest = get(),
            autosellRequest  = get(),
            closetRequest    = get(),
            storageRequest   = get(),
            banishManager    = get(),
            httpClient       = get(),
            hermitRequest    = get(),
            thriftyRequest   = get(),
            standardRequest  = get(),
            trendyRequest    = get(),
            displayCaseRequest = get(),
            clanStashRequest    = get(),
            mallManager         = get(),
            retrieveItemService = get(),
            outfitManager       = get(),
            equipmentRequest    = get(),
            equipmentManager    = get(),
            coinmasterManager   = get(),
            craftRequest        = get(),
            pulverizeRequest    = get(),
            zapRequest          = get(),
            untinkerRequest     = get(),
            manageStoreRequest  = get(),
            mallPriceManager    = get(),
            characterRequest    = get(),
            recoveryManager     = get(),
            adventureRequest    = get(),
            uneffectRequest     = get(),
            questDatabase       = get(),
            questLogRequest     = get(),
            clanLoungeRequest   = get(),
            familiarRequest     = get(),
            chatSender          = get(),
            maximizerManager    = get(),
            sessionLogger       = get(),
            eventBus            = get(),
            breakfastManager    = get(),
            sendMailRequest     = get(),
            sendGiftRequest     = get(),
            choiceRequest       = get(),
            hashingViseRequest  = get(),
            edServantManager    = get(),
            vykeaCompanionManager = get(),
            pastaThrallManager    = get(),
            adventureSpentTracker = get(),
            dreadKissesTracker    = get(),
            wildfireCampManager   = get(),
            summoningChamberManager = get(),
            alliedRadioManager = get(),
            cargoPocketSync = get(),
            cargoCultManager = get(),
            yegDemonNameSync = get(),
            demonInCombatNameSync = get(),
            demonNamesManager = get(),
            cleanupJunkRunner = get(),
            autoMallRunner = get(),
            quarkRunner = get(),
            buffBotManager = get(),
            buffBotDatabase = get(),
            faxBotManager = get(),
            faxBotDatabase = get(),
            chatProbe = get(),
            chatManager = get(),
            researchBenchRequest = get(),
            gourdRequest = get(),
            concoctionQueueRunner = get(),
            concoctionCreateRequest = get(),
            modeableRequest = get(),
            horseryRequest = get(),
            boomBoxRequest = get(),
            mindControlRequest = get(),
            absorbRequest = get(),
        )
    }
    singleOf(::ScriptManager)
    singleOf(::ScriptHookRunner)
    single {
        SessionManager(
            loginRequest         = get(),
            characterRequest     = get(),
            character            = get(),
            preferences          = get(),
            inventoryManager     = get(),
            familiarManager      = get(),
            skillManager         = get(),
            effectManager        = get(),
            scriptManager        = get(),
            gameDatabase         = get(),
            dailyResourceTracker = get(),
            questLogRequest      = get(),
            moodManager          = get(),
            banishManager        = get(),
            breakfastManager     = get(),
            outfitManager        = get(),
            sessionLogger        = get(),
            gameRuntimeLibrary   = get(),
            junkListManager      = get(),
            httpClient           = get(),
            closetRequest        = get(),
            storageRequest       = get(),
            clanStashRequest     = get(),
            displayCaseRequest   = get(),
        )
    }
    singleOf(::ShopRequest)
    singleOf(::CoinmasterRequest)
    singleOf(::MallSearchRequest)
    singleOf(::MallPurchaseRequest)
    single { MallPriceManager() }
    singleOf(::NpcBuyRequest)
    single { MallManager(get(), get(), get(), get()) }
    single {
        CraftRequest(
            client = get(),
            inventoryManager = get(),
            preferences = get(),
            character = get(),
            sessionLogger = get(),
        )
    }
    single {
        CoinmasterManager(
            coinmasterRequest = get(),
            inventoryManager = get(),
            gameDatabase = get(),
            client = get(),
            character = get(),
            preferences = get(),
            sessionLogger = get(),
        )
    }
    single {
        RetrieveItemService(
            inventoryManager = get(),
            closetRequest    = get(),
            storageRequest   = get(),
            displayCaseRequest = get(),
            clanStashRequest = get(),
            npcBuyRequest    = get(),
            mallManager      = get(),
            coinmasterManager = get(),
            craftRequest     = get(),
            useItemRequest   = get(),
            gameDatabase     = get(),
            hermitRequest    = get(),
            familiarRequest  = get(),
            character        = get(),
            preferences      = get(),
            standardRequest  = get(),
            thriftyRequest   = get(),
            trendyRequest    = get(),
            specialtyCreateProvider = { get() },
            createItemIngredientsProvider = { get() },
            equipmentRequest = get(),
            familiarManager = get(),
            untinkerRequest = get(),
        )
    }
    single {
        CreateItemIngredients(
            retrieveItemService = get(),
            gameDatabase = get(),
        )
    }
    single {
        OutfitManager(
            retrieveItemService = get(),
            equipmentRequest = get(),
            customOutfitRequest = get(),
            character = get(),
            gameDatabase = get(),
            closetRequest = get(),
            storageRequest = get(),
            displayCaseRequest = get(),
            clanStashRequest = get(),
            inventoryManager = get(),
            equipmentManager = get(),
        )
    }
    singleOf(::ChatManager)
    singleOf(::ChatSender)
    singleOf(::ChatProbe)
    singleOf(::ChatPoller)
    single { BuffBotDatabase.instance }
    singleOf(::BuffBotManager)
    single { FaxBotDatabase.instance }
    single {
        FaxBotManager(
            chatSender = get(),
            chatPoller = get(),
            chatManager = get(),
            clanLoungeRequest = get(),
            database = get(),
            gameDatabase = get(),
            preferences = get(),
            inventoryManager = get(),
            character = get(),
            chatProbe = get(),
        )
    }
}
