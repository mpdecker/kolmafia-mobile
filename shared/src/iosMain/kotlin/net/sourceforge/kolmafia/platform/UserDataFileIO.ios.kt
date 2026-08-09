package net.sourceforge.kolmafia.platform

import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile

private fun userDataBase(): String =
    UserDataFilePaths.testBasePath ?: (NSHomeDirectory() + "/Documents/data")

actual fun readUserDataText(relativePath: String): String? {
    val fullPath = "${userDataBase()}/$relativePath"
    val manager = NSFileManager.defaultManager
    if (!manager.fileExistsAtPath(fullPath)) return null
    return manager.contentsAtPath(fullPath)?.let { data ->
        NSString.create(data = data, encoding = NSUTF8StringEncoding) as String
    }
}

actual fun writeUserDataText(relativePath: String, text: String) {
    val fullPath = "${userDataBase()}/$relativePath"
    val slash = fullPath.lastIndexOf('/')
    if (slash > 0) {
        NSFileManager.defaultManager.createDirectoryAtPath(
            fullPath.substring(0, slash),
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
    }
    (text as NSString).writeToFile(
        fullPath,
        atomically = true,
        encoding = NSUTF8StringEncoding,
        error = null,
    )
}
