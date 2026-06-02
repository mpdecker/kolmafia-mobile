package net.sourceforge.kolmafia.di

import io.ktor.client.*
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.http.createKoLHttpClient
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.LoginRequest
import net.sourceforge.kolmafia.session.SessionManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val sharedModule = module {
    single<HttpClient> { createKoLHttpClient() }
    single { KoLCharacter() }
    single { Preferences(get()) }   // Settings provided by platformModule
    singleOf(::LoginRequest)
    singleOf(::CharacterRequest)
    singleOf(::SessionManager)
}
