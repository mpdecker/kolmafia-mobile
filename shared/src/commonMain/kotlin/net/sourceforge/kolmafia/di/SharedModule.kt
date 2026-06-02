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
}
