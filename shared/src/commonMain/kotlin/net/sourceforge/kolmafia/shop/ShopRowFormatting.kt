package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase

/** Desktop AdventureResult / ShopRow toData string formatting for session-log spading output. */
internal object ShopRowFormatting {

    fun formatStack(stack: ItemStack): String {
        if (stack.isMeat) {
            return "${formatCount(stack.count)} Meat"
        }
        if (stack.isSkill) {
            val name = SkillDefinitionDatabase.getById(stack.itemId)?.name ?: "skill ${stack.itemId}"
            return name
        }
        val name = ItemDatabase.getById(stack.itemId)?.name ?: "item ${stack.itemId}"
        return if (stack.count == 1) name else "$name (${stack.count})"
    }

    /** Desktop [ShopRow.toData] coinmasters.txt format: ShopName\\tROW123\\titem\\tcost… */
    fun toCoinmasterData(shopName: String, row: ShopRow): String =
        buildString {
            append(shopName)
            append('\t')
            append("ROW")
            append(row.rowId)
            append('\t')
            append(formatStack(row.item))
            for (cost in row.costs) {
                append('\t')
                append(formatStack(cost))
            }
        }

    /** Desktop concoctions.txt format: item\\tTYPE, ROWn\\tcost… */
    fun toConcoctionData(craftingType: String, row: ShopRow): String =
        buildString {
            append(formatStack(row.item))
            append('\t')
            append(craftingType)
            append(", ROW")
            append(row.rowId)
            for (cost in row.costs) {
                append('\t')
                append(formatStack(cost))
            }
        }

    /** Desktop coinmasters.txt legacy buy line: ShopName\\tbuy\\tprice\\titem\\tROWn */
    fun toLegacyBuyData(shopName: String, row: ShopRow): String {
        val cost = row.costs.firstOrNull()
            ?: return toCoinmasterData(shopName, row)
        return buildString {
            append(shopName)
            append("\tbuy\t")
            append(cost.count)
            append('\t')
            append(formatStack(row.item))
            append("\tROW")
            append(row.rowId)
        }
    }

    /** Desktop coinmasters.txt legacy sell line: ShopName\\tsell\\titemCount\\tprice\\tROWn */
    fun toLegacySellData(shopName: String, row: ShopRow): String {
        val cost = row.costs.firstOrNull()
            ?: return toCoinmasterData(shopName, row)
        return buildString {
            append(shopName)
            append("\tsell\t")
            append(row.item.count)
            append('\t')
            append(formatStack(cost))
            append("\tROW")
            append(row.rowId)
        }
    }

    /** Desktop [ShopRowData.dataString] shoprows.txt format: row\\tshopId\\titem\\tcost… */
    fun toShopRowData(rowId: Int, shopId: String, row: ShopRow): String =
        buildString {
            append(rowId)
            append('\t')
            append(shopId)
            append('\t')
            append(formatStack(row.item))
            for (cost in row.costs) {
                append('\t')
                append(formatStack(cost))
            }
        }

    private fun formatCount(count: Int): String =
        if (count >= 1000) {
            val chars = count.toString().toCharArray()
            buildString {
                for (i in chars.indices) {
                    if (i > 0 && (chars.size - i) % 3 == 0) append(',')
                    append(chars[i])
                }
            }
        } else {
            count.toString()
        }
}
