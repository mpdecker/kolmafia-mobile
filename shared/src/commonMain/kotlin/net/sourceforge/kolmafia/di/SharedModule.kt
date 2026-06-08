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
import net.sourceforge.kolmafia.session.BreakfastManager
import net.sourceforge.kolmafia.character.DailyResourceTracker
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.request.QuestLogRequest
import net.sourceforge.kolmafia.session.GoalManager
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.ash.ScriptManager
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.http.createKoLHttpClient
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.LoginRequest
import net.sourceforge.kolmafia.session.SessionManager
import net.sourceforge.kolmafia.buffbot.BuffBotDatabase
import net.sourceforge.kolmafia.buffbot.BuffBotManager
import net.sourceforge.kolmafia.chat.ChatManager
import net.sourceforge.kolmafia.chat.ChatPoller
import net.sourceforge.kolmafia.chat.ChatSender
import net.sourceforge.kolmafia.mall.MallPriceManager
import net.sourceforge.kolmafia.mall.MallPurchaseRequest
import net.sourceforge.kolmafia.mall.MallSearchRequest
import net.sourceforge.kolmafia.request.AutosellRequest
import net.sourceforge.kolmafia.request.ChewRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.DrinkBoozeRequest
import net.sourceforge.kolmafia.request.EatFoodRequest
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.request.UseItemRequest
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
    singleOf(::QuestLogRequest)
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
    singleOf(::EatFoodRequest)
    singleOf(::DrinkBoozeRequest)
    singleOf(::ChewRequest)
    singleOf(::AutosellRequest)
    singleOf(::ClosetRequest)
    singleOf(::StorageRequest)
    singleOf(::BreakfastManager)
    singleOf(::InventoryManager)
    singleOf(::FamiliarManager)
    singleOf(::SkillCastRequest)
    singleOf(::SkillManager)
    singleOf(::RecoveryManager)
    singleOf(::UneffectRequest)
    single { MoodManager(skillManager = get(), preferences = get(), uneffectRequest = get()) }
    singleOf(::ManaBurnManager)
    singleOf(::BanishManager)
    singleOf(::EffectManager)
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
            httpClient        = get(),
        )
    }
    singleOf(::ScriptManager)
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
        )
    }
    singleOf(::ShopRequest)
    singleOf(::CoinmasterRequest)
    singleOf(::MallSearchRequest)
    singleOf(::MallPurchaseRequest)
    single { MallPriceManager() }
    singleOf(::ChatManager)
    singleOf(::ChatSender)
    singleOf(::ChatPoller)
    single { BuffBotDatabase.default }
    singleOf(::BuffBotManager)
}
