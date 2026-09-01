package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/** Read-only Slimeling stack accounting used by the `slime-stack` CLI command. */
object SlimeStackManager {
    const val STACKS_DROPPED_PREF = "slimelingStacksDropped"
    const val STACKS_DUE_PREF = "slimelingStacksDue"

    fun status(preferences: Preferences): String {
        val got = preferences.getInt(STACKS_DROPPED_PREF, 0)
        val due = preferences.getInt(STACKS_DUE_PREF, 0)
        return when {
            due <= 0 ->
                "No slime stacks due. Feed your Slimeling with basic meat equipment or Gnollish autoplungers to receive slime stacks."
            got >= due ->
                "Got all $due expected slime stacks this ascension. Feed your Slimeling with basic meat equipment or Gnollish autoplungers to receive more."
            else -> {
                val missing = due - got
                val next = got + 1
                "$missing slime stacks queued. Next: #$next (expected after " +
                    "${next * (next + 1) / 2} total Slimeling combats)."
            }
        }
    }
}
