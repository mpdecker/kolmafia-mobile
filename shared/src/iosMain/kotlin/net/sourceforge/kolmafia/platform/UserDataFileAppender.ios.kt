package net.sourceforge.kolmafia.platform

import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.writeToFile

actual fun appendUserDataLine(relativePath: String, line: String) {
    val base = UserDataFilePaths.testBasePath
        ?: (NSHomeDirectory() + "/Documents/data")
    val fullPath = "$base/$relativePath"
    val slash = fullPath.lastIndexOf('/')
    if (slash > 0) {
        NSFileManager.defaultManager.createDirectoryAtPath(
            fullPath.substring(0, slash),
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
    }
    val existing = readExisting(fullPath)
    val content = existing + line + "\n"
    (content as NSString).writeToFile(
        fullPath,
        atomically = true,
        encoding = NSUTF8StringEncoding,
        error = null,
    )
}

private fun readExisting(path: String): String {
    val manager = NSFileManager.defaultManager
    if (!manager.fileExistsAtPath(path)) return ""
    return manager.contentsAtPath(path)?.let { data ->
        NSString.create(data = data, encoding = NSUTF8StringEncoding) as String
    } ?: ""
}
