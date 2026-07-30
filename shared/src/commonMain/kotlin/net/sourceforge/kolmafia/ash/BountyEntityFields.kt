package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.BountyDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.modifiers.LocationNames
import net.sourceforge.kolmafia.modifiers.MonsterNames

/**
 * Resolves `$bounty[field]` bracket access. Mirrors desktop BountyProxy metadata.
 */
internal object BountyEntityFields {

    fun resolve(
        bountyRef: String,
        fieldName: String,
        gameDatabase: GameDatabase?,
    ): AshValue {
        val bountyName = BountyDatabase.resolve(bountyRef) ?: bountyRef.trim().takeIf { it.isNotEmpty() }
        val bounty = bountyName?.let { BountyDatabase.getByName(it) }

        return when (fieldName.lowercase()) {
            "plural" -> AshValue.of(bounty?.plural ?: "")
            "type" -> AshValue.of(bounty?.typeString() ?: "")
            "kol_internal_type" -> AshValue.of(bounty?.kolInternalType() ?: "")
            "number" -> AshValue.of((bounty?.count ?: 0).toLong())
            "image" -> AshValue.of(bounty?.image ?: "")
            "monster" -> AshValue(
                AshType.MONSTER,
                bounty?.monster?.let { MonsterNames.resolve(it, gameDatabase) ?: it } ?: "",
            )
            "location" -> AshValue(
                AshType.LOCATION,
                bounty?.bestLocation?.let { LocationNames.resolve(it) ?: it } ?: "",
            )
            else -> throw ScriptException("bounty has no field '$fieldName'")
        }
    }
}
