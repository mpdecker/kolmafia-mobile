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
import net.sourceforge.kolmafia.request.ClanRumpusRequest
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.LoginRequest
import net.sourceforge.kolmafia.request.ManageStoreRequest
import net.sourceforge.kolmafia.session.SessionManager
import net.sourceforge.kolmafia.buffbot.BuffBotDatabase
import net.sourceforge.kolmafia.buffbot.BuffBotManager
import net.sourceforge.kolmafia.chat.ChatManager
import net.sourceforge.kolmafia.chat.ChatPoller
import net.sourceforge.kolmafia.chat.ChatSender
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPriceManager
import net.sourceforge.kolmafia.mall.MallPurchaseRequest
import net.sourceforge.kolmafia.mall.MallSearchRequest
import net.sourceforge.kolmafia.npc.NpcBuyRequest
import net.sourceforge.kolmafia.request.AutosellRequest
import net.sourceforge.kolmafia.request.PulverizeRequest
import net.sourceforge.kolmafia.request.UntinkerRequest
import net.sourceforge.kolmafia.request.ZapRequest
import net.sourceforge.kolmafia.request.ChewRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.request.CustomOutfitRequest
import net.sourceforge.kolmafia.request.CraftRequest
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.request.DrinkBoozeRequest
import net.sourceforge.kolmafia.request.EatFoodRequest
import net.sourceforge.kolmafia.request.SendGiftRequest
import net.sourceforge.kolmafia.request.SendMailRequest
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.request.HermitRequest
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.request.TrendyRequest
import net.sourceforge.kolmafia.request.ThriftyRequest
import net.sourceforge.kolmafia.request.UseItemRequest
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
    singleOf(::AdventureRequest)
    singleOf(::FightRequest)
    singleOf(::ChoiceRequest)
    single { GoalManager() }
    single { QuestDatabase(get()) }
    single { QuestLogRequest(get(), get(), get()) }
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
    singleOf(::UseItemRequest)
    singleOf(::HermitRequest)
    singleOf(::ThriftyRequest)
    singleOf(::StandardRequest)
    singleOf(::TrendyRequest)
    singleOf(::EatFoodRequest)
    singleOf(::DrinkBoozeRequest)
    singleOf(::ChewRequest)
    singleOf(::AutosellRequest)
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
    singleOf(::EquipmentRequest)
    singleOf(::CustomOutfitRequest)
    singleOf(::CampgroundRequest)
    singleOf(::ClanRumpusRequest)
    singleOf(::ClanLoungeRequest)
    singleOf(::FamiliarRequest)
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
    singleOf(::FamiliarManager)
    singleOf(::SkillCastRequest)
    singleOf(::SkillManager)
    singleOf(::RecoveryManager)
    singleOf(::UneffectRequest)
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
    singleOf(::EffectManager)
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
            preferences      = get(),
            gameDatabase     = get(),
            useItemRequest   = get(),
            eatFoodRequest   = get(),
            drinkBoozeRequest = get(),
            chewRequest      = get(),
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
            breakfastManager    = get(),
            sendMailRequest     = get(),
            sendGiftRequest     = get(),
            choiceRequest       = get(),
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
        )
    }
    singleOf(::ShopRequest)
    singleOf(::CoinmasterRequest)
    singleOf(::MallSearchRequest)
    singleOf(::MallPurchaseRequest)
    single { MallPriceManager() }
    singleOf(::NpcBuyRequest)
    single { MallManager(get(), get(), get(), get()) }
    singleOf(::CraftRequest)
    single {
        CoinmasterManager(
            coinmasterRequest = get(),
            inventoryManager = get(),
            gameDatabase = get(),
            client = get(),
            character = get(),
            preferences = get(),
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
        )
    }
    singleOf(::ChatManager)
    singleOf(::ChatSender)
    singleOf(::ChatPoller)
    single { BuffBotDatabase.instance }
    singleOf(::BuffBotManager)
}
