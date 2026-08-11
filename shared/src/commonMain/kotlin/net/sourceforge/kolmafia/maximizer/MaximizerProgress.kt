package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.ash.currentTimeMillis

/** Desktop `MaximizerSpeculation.showProgress()` throttled status updates (Phase 407). */
object MaximizerProgress {
    private const val THROTTLE_MS = 5_000L

    private var nextDisplayAtMs = 0L
    private var _lastMessage: String? = null

    /** Injectable clock for throttle tests. */
    var clockMs: () -> Long = { currentTimeMillis() }

    /** Live progress sink; set by [MaximizerManager.buildMaximizePlan]. */
    var sink: (String) -> Unit = {}

    val lastMessage: String?
        get() = _lastMessage

    fun reset() {
        nextDisplayAtMs = 0L
        _lastMessage = null
    }

    fun format(checked: Int, score: Double, failed: Boolean): String {
        val rounded = kotlin.math.round(score * 100.0) / 100.0
        val base = "$checked combinations checked, best score ${"%.2f".format(rounded)}"
        return if (failed) "$base (FAIL)" else base
    }

    fun maybeShow(
        checked: Int,
        score: Double,
        failed: Boolean,
        display: (String) -> Unit = sink,
    ) {
        val now = clockMs()
        if (now < nextDisplayAtMs) return
        nextDisplayAtMs = now + THROTTLE_MS
        emit(checked, score, failed, display)
    }

    fun showFinal(
        checked: Int,
        score: Double,
        failed: Boolean,
        display: (String) -> Unit = sink,
    ) {
        emit(checked, score, failed, display)
    }

    private fun emit(
        checked: Int,
        score: Double,
        failed: Boolean,
        display: (String) -> Unit,
    ) {
        val message = format(checked, score, failed)
        _lastMessage = message
        display(message)
    }
}
