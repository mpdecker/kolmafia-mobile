package net.sourceforge.kolmafia.inventory

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.preferences.Preferences

data class AccessCountContext(
    val characterState: CharacterState? = null,
    val gameDatabase: GameDatabase? = null,
    val familiarManager: FamiliarManager? = null,
    val preferences: Preferences? = null,
)
