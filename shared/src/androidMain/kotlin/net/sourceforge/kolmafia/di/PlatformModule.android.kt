package net.sourceforge.kolmafia.di

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.Settings
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<Settings> {
        SharedPreferencesSettings(
            androidContext().getSharedPreferences("kolmafia_prefs", Context.MODE_PRIVATE)
        )
    }
}
