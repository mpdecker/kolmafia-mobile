package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerSessionLog(scope: AshScope) {
    val stringArrayType = AggregateType(AshType.INT, AshType.STRING)
    // Desktop session_logs(dayCount): negative throws; 0 → empty; N → N daily files
    regFn(scope, "session_logs", stringArrayType, listOf("days" to AshType.INT)) { _, args ->
        val days = args[0].toLong().toInt()
        if (days < 0) {
            throw ScriptException("Can't get session logs for a negative number of days")
        }
        val player = character?.state?.value?.name.orEmpty()
        if (days >= 1) {
            val fromFiles = sessionLogsForDays(player, days)
            val anyFile = (0 until days).any { i ->
                fromFiles[AshValue.of(i.toLong())].toString().isNotBlank()
            }
            if (anyFile) return@regFn fromFiles
            // Fall back to in-memory SessionLogger buffer when no daily files exist.
            val lines = sessionLogger?.recentLines(days) ?: emptyList()
            if (lines.isNotEmpty()) {
                val result = AggregateValue(stringArrayType)
                lines.forEachIndexed { index, line ->
                    result[AshValue.of(index.toLong())] = AshValue.of(line)
                }
                return@regFn result
            }
        }
        sessionLogsForDays(player, days)
    }
}
