package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.character.MainStat
import net.sourceforge.kolmafia.request.GourdRequest

/** Gourd quest orchestration shared by CLI and adventure automation. */
object GourdManager {
    fun gourdItemId(mainStat: MainStat): Int = when (mainStat) {
        MainStat.MUSCLE -> ItemPool.KNOB_FIRECRACKER
        MainStat.MYSTICALITY -> ItemPool.CAN_LID
        MainStat.MOXIE -> ItemPool.SPIDER_WEB
    }

    suspend fun tradeGourdItems(request: GourdRequest, maxTrades: Int = 21): Int {
        request.visit().getOrElse { return 0 }
        var trades = 0
        while (trades < maxTrades && request.trade().isSuccess) trades++
        return trades
    }

    suspend fun visit(request: GourdRequest): Result<String> = request.visit()

    fun status(nextCount: Int, itemName: String, available: Int): String =
        "Gourd quest needs $nextCount $itemName(s); you have $available."
}
