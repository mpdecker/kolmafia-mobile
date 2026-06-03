package net.sourceforge.kolmafia.di

import io.ktor.client.*
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.adventure.AdventureRequest
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.adventure.FightRequest
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
import net.sourceforge.kolmafia.shop.CoinmasterRequest
import net.sourceforge.kolmafia.shop.ShopRequest
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val sharedModule = module {
    single<HttpClient> { createKoLHttpClient() }
    single { KoLCharacter() }
    single { Preferences(get()) }
    single { GameEventBus() }
    singleOf(::LoginRequest)
    singleOf(::CharacterRequest)
    singleOf(::AdventureRequest)
    singleOf(::FightRequest)
    singleOf(::ChoiceRequest)
    singleOf(::AdventureManager)
    singleOf(::InventoryManager)
    singleOf(::FamiliarManager)
    singleOf(::SkillCastRequest)
    singleOf(::SkillManager)
    singleOf(::EffectManager)
    singleOf(::SessionManager)
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
