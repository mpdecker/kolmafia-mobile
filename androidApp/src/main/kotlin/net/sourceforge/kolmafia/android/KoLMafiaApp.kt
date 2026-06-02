package net.sourceforge.kolmafia.android

import android.app.Application
import net.sourceforge.kolmafia.di.initKoin
import org.koin.android.ext.koin.androidContext

class KoLMafiaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@KoLMafiaApp)
        }
    }
}
