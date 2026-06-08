package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryTimingTest {

    private val lib get() = GameRuntimeLibrary.forTesting()

    @Test
    fun wait_zeroSecondsDoesNotThrow() {
        runLib(lib, "wait(0);")
    }

    @Test
    fun waitq_zeroSecondsDoesNotThrow() {
        runLib(lib, "waitq(0);")
    }

    @Test
    fun wait_isCallableFromAsh() {
        runLib(lib, "int n = 0; wait(n);")
    }

    @Test
    fun logprint_outputsMessage() {
        assertEquals("hello log", outputLib(lib, """logprint("hello log");"""))
    }

    @Test
    fun debugprint_outputsMessage() {
        assertEquals("debug msg", outputLib(lib, """debugprint("debug msg");"""))
    }

    @Test
    fun traceprint_outputsMessage() {
        assertEquals("trace msg", outputLib(lib, """traceprint("trace msg");"""))
    }

    @Test
    fun logprint_returnsVoid() {
        runLib(lib, """logprint("test"); print("ok");""")
    }
}
