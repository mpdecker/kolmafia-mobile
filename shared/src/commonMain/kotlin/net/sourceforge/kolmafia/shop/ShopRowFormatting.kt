package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.data.ItemDatabase

/** Desktop AdventureResult / ShopRow toData string formatting for session-log spading output. */
internal object ShopRowFormatting {

    fun formatStack(stack: ItemStack): String {
        if (stack.isMeat) {
            return "${formatCount(stack.count)} Meat"
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
