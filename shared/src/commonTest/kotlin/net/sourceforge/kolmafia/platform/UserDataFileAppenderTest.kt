package net.sourceforge.kolmafia.platform

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserDataFileAppenderTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setUp() {
        tempDir = File.createTempFile("kolmafia-userdata-", "").apply {
            delete()
            mkdirs()
        }
        UserDataFilePaths.testBasePath = tempDir.absolutePath
    }

    @AfterTest
    fun tearDown() {
        UserDataFilePaths.testBasePath = null
        tempDir.deleteRecursively()
    }

    @Test
    fun appendLine_createsFileAndAppendsSecondLine() {
        UserDataFileAppender.appendLine("nested/test.txt", "line-one")
        UserDataFileAppender.appendLine("nested/test.txt", "line-two")
        val file = File(tempDir, "nested/test.txt")
        assertTrue(file.exists())
        assertEquals("line-one\nline-two\n", file.readText())
    }
}
