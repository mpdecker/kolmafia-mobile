package net.sourceforge.kolmafia.platform

import java.io.File

private fun userDataBase(): String? =
    UserDataFilePaths.testBasePath
        ?: (System.getProperty("java.io.tmpdir") + "/kolmafia-data")

actual fun readUserDataText(relativePath: String): String? {
    val base = userDataBase() ?: return null
    val file = File(base, relativePath)
    if (!file.exists()) return null
    return file.readText()
}

actual fun writeUserDataText(relativePath: String, text: String) {
    val base = userDataBase() ?: return
    val file = File(base, relativePath)
    file.parentFile?.mkdirs()
    file.writeText(text)
}
