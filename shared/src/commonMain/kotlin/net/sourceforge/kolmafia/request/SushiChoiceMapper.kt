package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.data.ItemDatabase

/** Desktop [net.sourceforge.kolmafia.request.concoction.SushiRequest] name→form mapping. */
object SushiChoiceMapper {
    private data class BaseSushi(val id: Int, val name: String)

    private data class Topping(val prefix: String, val itemName: String)

    private data class Filling(val finalName: String, val baseName: String, val itemName: String)

    private data class Veggie(val prefix: String, val itemName: String)

    private data class Dipping(val suffix: String, val itemName: String)

    private val BASE_SUSHI = listOf(
        BaseSushi(1, "beefy nigiri"),
        BaseSushi(2, "glistening nigiri"),
        BaseSushi(3, "slick nigiri"),
        BaseSushi(4, "beefy maki"),
        BaseSushi(5, "glistening maki"),
        BaseSushi(6, "slick maki"),
        BaseSushi(7, "bento box"),
    )

    private val TOPPING = listOf(
        Topping("salty", "sea salt crystal"),
        Topping("magical", "dragonfish caviar"),
        Topping("electric", "eel sauce"),
        Topping("Yuletide", "peppermint eel sauce"),
    )

    private val FILLING1 = listOf(
        Filling("giant dragon roll", "beefy maki", "sea cucumber"),
        Filling("musclebound rabbit roll", "beefy maki", "sea carrot"),
        Filling("python roll", "beefy maki", "sea avocado"),
        Filling("Jack LaLanne roll", "beefy maki", "sea radish"),
        Filling("jacked Santa roll", "beefy maki", "green and red bean"),
        Filling("wise dragon roll", "glistening maki", "sea cucumber"),
        Filling("white rabbit roll", "glistening maki", "sea carrot"),
        Filling("ancient serpent roll", "glistening maki", "sea avocado"),
        Filling("wizened master roll", "glistening maki", "sea radish"),
        Filling("omniscient Santa roll", "glistening maki", "green and red bean"),
        Filling("tricky dragon roll", "slick maki", "sea cucumber"),
        Filling("sneaky rabbit roll", "slick maki", "sea carrot"),
        Filling("slippery snake roll", "slick maki", "sea avocado"),
        Filling("eleven oceans roll", "slick maki", "sea radish"),
        Filling("sneaky Santa roll", "slick maki", "green and red bean"),
    )

    private val VEGGIE = listOf(
        Veggie("tempura avocado", "tempura avocado"),
        Veggie("tempura broccoli", "tempura broccoli"),
        Veggie("tempura carrot", "tempura carrot"),
        Veggie("tempura cauliflower", "tempura cauliflower"),
        Veggie("tempura cucumber", "tempura cucumber"),
        Veggie("tempura green and red bean", "tempura green and red bean"),
        Veggie("tempura radish", "tempura radish"),
    )

    private val DIPPING = listOf(
        Dipping("anemone sauce", "anemone sauce"),
        Dipping("eel sauce", "eel sauce"),
        Dipping("inky squid sauce", "inky squid sauce"),
        Dipping("Mer-kin weaksauce", "Mer-kin weaksauce"),
        Dipping("peanut sauce", "peanut sauce"),
        Dipping("peppermint eel sauce", "peppermint eel sauce"),
    )

    fun resultNameFromFormFields(fields: Map<String, String>): String? {
        val sushiId = fields["whichsushi"]?.toIntOrNull() ?: return null
        var name = idToName(sushiId) ?: return null

        if (sushiId == 7) {
            fields["veggie"]?.toIntOrNull()?.let { veggieId ->
                name = veggieToName(name, veggieId)
            }
            fields["dipping"]?.toIntOrNull()?.let { dippingId ->
                name = dippingToName(name, dippingId)
            }
        } else {
            fields["whichfilling1"]?.toIntOrNull()?.let { fillingId ->
                name = filling1ToName(name, fillingId)
            }
            fields["whichtopping"]?.toIntOrNull()?.let { toppingId ->
                name = toppingToName(name, toppingId)
            }
        }

        return name
    }

    fun formFields(resultName: String): Map<String, String>? {
        val sushiId = nameToId(resultName)
        if (sushiId <= 0) return null

        val fields = linkedMapOf(
            "action" to "Yep.",
            "whichsushi" to sushiId.toString(),
        )

        nameToTopping(resultName)?.let { itemId ->
            fields["whichtopping"] = itemId.toString()
        }
        nameToFilling1(resultName)?.let { itemId ->
            fields["whichfilling1"] = itemId.toString()
        }
        nameToVeggie(resultName)?.let { itemId ->
            fields["veggie"] = itemId.toString()
        }
        nameToDipping(resultName)?.let { itemId ->
            fields["dipping"] = itemId.toString()
        }

        return fields
    }

    private fun nameToId(name: String): Int {
        if (name.contains("bento box", ignoreCase = true)) {
            return 7
        }

        BASE_SUSHI.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it.id }

        FILLING1.firstOrNull { name.contains(it.finalName, ignoreCase = true) }?.let { filling ->
            return nameToId(filling.baseName)
        }

        TOPPING.firstOrNull { name.startsWith(it.prefix) }?.let { topping ->
            val index = name.indexOf(' ')
            if (index != -1) {
                return nameToId(name.substring(index + 1))
            }
        }

        return -1
    }

    private fun idToName(id: Int): String? = BASE_SUSHI.firstOrNull { it.id == id }?.name

    private fun toppingToName(baseName: String, toppingItemId: Int): String {
        val topping = TOPPING.firstOrNull { itemIdFor(it.itemName) == toppingItemId } ?: return baseName
        return "${topping.prefix} $baseName"
    }

    private fun filling1ToName(baseName: String, fillingItemId: Int): String {
        return FILLING1.firstOrNull {
            it.baseName.equals(baseName, ignoreCase = true) &&
                itemIdFor(it.itemName) == fillingItemId
        }?.finalName ?: baseName
    }

    private fun veggieToName(baseName: String, veggieItemId: Int): String {
        val veggie = VEGGIE.firstOrNull { itemIdFor(it.itemName) == veggieItemId } ?: return baseName
        return "${veggie.prefix} $baseName"
    }

    private fun dippingToName(baseName: String, dippingItemId: Int): String {
        val dipping = DIPPING.firstOrNull { itemIdFor(it.itemName) == dippingItemId } ?: return baseName
        return "$baseName with ${dipping.suffix}"
    }

    private fun nameToTopping(name: String): Int? {
        val topping = TOPPING.firstOrNull { name.startsWith(it.prefix) } ?: return null
        return itemIdFor(topping.itemName)
    }

    private fun nameToFilling1(name: String): Int? {
        val filling = FILLING1.firstOrNull { name.contains(it.finalName, ignoreCase = true) } ?: return null
        return itemIdFor(filling.itemName)
    }

    private fun nameToVeggie(name: String): Int? {
        val veggie = VEGGIE.firstOrNull { name.startsWith(it.prefix, ignoreCase = true) } ?: return null
        return itemIdFor(veggie.itemName)
    }

    private fun nameToDipping(name: String): Int? {
        val dipping = DIPPING.firstOrNull { name.endsWith(it.suffix, ignoreCase = true) } ?: return null
        return itemIdFor(dipping.itemName)
    }

    private fun itemIdFor(itemName: String): Int? = ItemDatabase.getByName(itemName)?.id
}
