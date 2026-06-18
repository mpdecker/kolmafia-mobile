package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.modifiers.ServantData
import net.sourceforge.kolmafia.servant.EdServantManager
import net.sourceforge.kolmafia.servant.EdServantRecord

/**
 * Resolves `$servant[field]` bracket access. Mirrors desktop [ServantProxy].
 */
internal object ServantEntityFields {

    fun resolve(servantType: String, fieldName: String, manager: EdServantManager?): AshValue {
        val catalog = ServantData.resolve(servantType)
        val record = manager?.findEdServant(servantType)
        return when (fieldName.lowercase()) {
            "id" -> AshValue.of((catalog?.id ?: 0).toLong())
            "name" -> AshValue.of(record?.name ?: "")
            "level" -> AshValue.of((record?.level ?: 0).toLong())
            "experience" -> AshValue.of((record?.experience ?: 0).toLong())
            "image" -> AshValue.of(catalog?.image ?: "")
            "level1_ability" -> AshValue.of(catalog?.level1Ability ?: "")
            "level7_ability" -> AshValue.of(catalog?.level7Ability ?: "")
            "level14_ability" -> AshValue.of(catalog?.level14Ability ?: "")
            "level21_ability" -> AshValue.of(catalog?.level21Ability ?: "")
            else -> throw ScriptException("servant has no field '$fieldName'")
        }
    }

    fun recordFor(manager: EdServantManager?, servantType: String): EdServantRecord? =
        manager?.findEdServant(servantType)
}
