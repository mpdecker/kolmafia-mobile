package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerTimingAndLogging(scope: AshScope) {

    // wait(secs: int) — pauses the script for [secs] seconds.
    regFn(scope, "wait", AshType.VOID, listOf("secs" to AshType.INT)) { _, args ->
        val ms = args[0].toLong() * 1000L
        if (ms > 0L) kotlinx.coroutines.runBlocking { kotlinx.coroutines.delay(ms) }
        AshValue.VOID
    }

    // waitq(secs: int) — same as wait; desktop distinction is logging verbosity only.
    regFn(scope, "waitq", AshType.VOID, listOf("secs" to AshType.INT)) { _, args ->
        val ms = args[0].toLong() * 1000L
        if (ms > 0L) kotlinx.coroutines.runBlocking { kotlinx.coroutines.delay(ms) }
        AshValue.VOID
    }

    // logprint / debugprint / traceprint — desktop routes these to different log channels.
    // Mobile has one output channel; all three route to the same print handler.
    regFn(scope, "logprint", AshType.VOID, listOf("msg" to AshType.STRING)) { runtime, args ->
        runtime.print(args[0].toString())
        AshValue.VOID
    }
    regFn(scope, "debugprint", AshType.VOID, listOf("msg" to AshType.STRING)) { runtime, args ->
        runtime.print(args[0].toString())
        AshValue.VOID
    }
    regFn(scope, "traceprint", AshType.VOID, listOf("msg" to AshType.STRING)) { runtime, args ->
        runtime.print(args[0].toString())
        AshValue.VOID
    }
}
