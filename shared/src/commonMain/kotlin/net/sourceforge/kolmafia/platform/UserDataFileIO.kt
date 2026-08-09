package net.sourceforge.kolmafia.platform

expect fun readUserDataText(relativePath: String): String?

expect fun writeUserDataText(relativePath: String, text: String)

object UserDataFileIO {
    fun readText(relativePath: String): String? = readUserDataText(relativePath)

    fun writeText(relativePath: String, text: String) = writeUserDataText(relativePath, text)
}
