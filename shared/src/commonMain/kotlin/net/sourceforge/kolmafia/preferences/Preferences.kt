package net.sourceforge.kolmafia.preferences

import com.russhwolf.settings.Settings

class Preferences(private val settings: Settings) {

    fun getString(key: String, default: String = ""): String =
        settings.getString(key, default)

    fun setString(key: String, value: String) =
        settings.putString(key, value)

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        settings.getBoolean(key, default)

    fun setBoolean(key: String, value: Boolean) =
        settings.putBoolean(key, value)

    fun getInt(key: String, default: Int = 0): Int =
        settings.getInt(key, default)

    fun setInt(key: String, value: Int) =
        settings.putInt(key, value)

    companion object Keys {
        const val LAST_USERNAME = "lastUsername"
    }
}
