package net.sourceforge.kolmafia.platform

expect fun appendUserDataLine(relativePath: String, line: String)

object UserDataFileAppender {
    fun appendLine(relativePath: String, line: String) {
        appendUserDataLine(relativePath, line)
    }
}
