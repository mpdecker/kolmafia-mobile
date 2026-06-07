package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.EquipmentSlot

internal fun GameRuntimeLibrary.registerEquipmentQueries(scope: AshScope) {

    fun resolveSlot(slotName: String): String? {
        val slot = EquipmentSlot.entries.find { s ->
            s.displayName.equals(slotName, ignoreCase = true)
                || s.apiKey.equals(slotName, ignoreCase = true)
        }
        val itemName = slot?.let { character?.state?.value?.equipment?.get(it) }
        return if (itemName.isNullOrBlank()) null else itemName
    }

    regFn(scope, "equipped_item", AshType.ITEM,
        listOf("slot" to AshType.SLOT)) { _, args ->
        AshValue.item(resolveSlot(args[0].toString()) ?: "none")
    }

    regFn(scope, "have_equipped", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM)) { _, args ->
        val name = args[0].toString()
        val has = character?.state?.value?.equipment?.values
            ?.any { it.equals(name, ignoreCase = true) } ?: false
        AshValue.of(has)
    }

    regFn(scope, "to_slot", AshType.SLOT,
        listOf("name" to AshType.STRING)) { _, args ->
        AshValue(AshType.SLOT, args[0].toString())
    }

    regFn(scope, "slot_to_item", AshType.ITEM,
        listOf("slot" to AshType.SLOT)) { _, args ->
        AshValue.item(resolveSlot(args[0].toString()) ?: "none")
    }
}
