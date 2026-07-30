package net.sourceforge.kolmafia.platform

import java.io.File

actual fun appendUserDataLine(relativePath: String, line: String) {
    val base = UserDataFilePaths.testBasePath
        ?: (System.getProperty("java.io.tmpdir") + "/kolmafia-data")
    val file = File(base, relativePath)
    file.parentFile?.mkdirs()
    file.appendText(line + "\n")
}
