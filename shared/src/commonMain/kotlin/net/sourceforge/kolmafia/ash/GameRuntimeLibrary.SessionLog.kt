package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerSessionLog(scope: AshScope) {
    val stringArrayType = AggregateType(AshType.INT, AshType.STRING)
    regFn(scope, "session_logs", stringArrayType, listOf("days" to AshType.INT)) { _, args ->
        val days = args[0].toLong().toInt().coerceAtLeast(1)
        val lines = sessionLogger?.recentLines(days) ?: emptyList()
        val result = AggregateValue(stringArrayType)
        lines.forEachIndexed { index, line ->
            result[AshValue.of(index.toLong())] = AshValue.of(line)
        }
        result
    }
}
