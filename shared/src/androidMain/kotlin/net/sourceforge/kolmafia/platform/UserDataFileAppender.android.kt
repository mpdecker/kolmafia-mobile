package net.sourceforge.kolmafia.platform

import android.content.Context
import java.io.File

object AndroidUserDataContext {
    var dataDirectory: String? = null

    fun init(context: Context) {
        dataDirectory = File(context.filesDir, "data").absolutePath
    }
}

actual fun appendUserDataLine(relativePath: String, line: String) {
    val base = UserDataFilePaths.testBasePath ?: AndroidUserDataContext.dataDirectory ?: return
    val file = File(base, relativePath)
    file.parentFile?.mkdirs()
    file.appendText(line + "\n")
}
