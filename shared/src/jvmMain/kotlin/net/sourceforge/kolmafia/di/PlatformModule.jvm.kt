package net.sourceforge.kolmafia.di

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.prefs.Preferences as JavaPreferences

actual val platformModule: Module = module {
    single<Settings> { PreferencesSettings(JavaPreferences.userRoot().node("kolmafia")) }
}
