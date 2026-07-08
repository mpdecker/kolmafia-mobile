package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.GameDatabase

/**
 * Resolves `$monster[field]` bracket access. Mirrors desktop [MonsterProxy].
 */
internal object MonsterEntityFields {

    fun resolve(monsterName: String, fieldName: String, gameDatabase: GameDatabase?): AshValue {
        val monster = gameDatabase?.monster(monsterName)
        return when (fieldName.lowercase()) {
            "id" -> AshValue.of((monster?.id ?: 0).toLong())
            "name" -> AshValue.of(monster?.name ?: "")
            "article" -> AshValue.of(monster?.article ?: "")
            "image" -> AshValue.of(monster?.image ?: "")
            "base_hp" -> AshValue.of((monster?.hp ?: 0).toLong())
            "base_attack" -> AshValue.of((monster?.attack ?: 0).toLong())
            "base_defense" -> AshValue.of((monster?.defense ?: 0).toLong())
            "base_initiative" -> AshValue.of((monster?.initiative ?: 0).toLong())
            "min_meat" -> AshValue.of((monster?.meatDrop ?: 0).toLong())
            "max_meat" -> AshValue.of((monster?.meatDrop ?: 0).toLong())
            "phylum" -> AshValue(AshType.PHYLUM, monster?.phylum ?: "")
            "boss" -> AshValue.of(monster?.isBoss ?: false)
            "ghost" -> AshValue.of(monster?.isGhost ?: false)
            "lucky" -> AshValue.of(monster?.isLucky ?: false)
            "copyable" -> AshValue.of(monster?.isCopyable ?: true)
            "wishable" -> AshValue.of(monster?.isWishable ?: true)
            else -> throw ScriptException("monster has no field '$fieldName'")
        }
    }
}
