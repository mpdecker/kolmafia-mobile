package net.sourceforge.kolmafia.platform

expect fun readUserDataText(relativePath: String): String?

expect fun writeUserDataText(relativePath: String, text: String)

/** File names (not paths) directly under [relativeDir], or empty when missing. */
expect fun listUserDataNames(relativeDir: String): List<String>

object UserDataFileIO {
    fun readText(relativePath: String): String? = readUserDataText(relativePath)

    fun writeText(relativePath: String, text: String) = writeUserDataText(relativePath, text)

    /** Desktop DataUtilities.list — names under a user-data subdirectory. */
    fun listNames(relativeDir: String): List<String> = listUserDataNames(relativeDir)
}
