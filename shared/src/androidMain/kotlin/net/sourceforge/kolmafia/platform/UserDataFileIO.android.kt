package net.sourceforge.kolmafia.platform

import java.io.File

private fun userDataBase(): String? =
    UserDataFilePaths.testBasePath ?: AndroidUserDataContext.dataDirectory

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

actual fun listUserDataNames(relativeDir: String): List<String> {
    val base = userDataBase() ?: return emptyList()
    val dir = File(base, relativeDir)
    if (!dir.isDirectory) return emptyList()
    return dir.list()?.toList().orEmpty()
}
