package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState

/** Ensures HC/thrifty/trendy restricted-item lists are loaded before restriction checks. */
object RestrictionListRefresh {

    suspend fun ensureInitialized(
        state: CharacterState?,
        standardRequest: StandardRequest? = null,
        thriftyRequest: ThriftyRequest? = null,
        trendyRequest: TrendyRequest? = null,
    ) {
        if (state == null) return
        if (state.isRestricted) {
            standardRequest?.ensureInitialized()
        }
        if (state.isThrifty) {
            thriftyRequest?.ensureInitialized()
        }
        if (state.isTrendy) {
            trendyRequest?.ensureInitialized()
        }
    }
}
