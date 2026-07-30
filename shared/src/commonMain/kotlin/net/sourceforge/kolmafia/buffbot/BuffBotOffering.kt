package net.sourceforge.kolmafia.buffbot

/** Desktop-aligned buffbot price row (may aggregate multiple buffs at the same meat price). */
data class BuffBotOffering(
    val botName: String,
    val price: Int,
    val philanthropic: Boolean,
    val buffs: List<String>,
    val turns: List<Int>,
) {
    init {
        require(buffs.size == turns.size) { "buffs and turns must align" }
    }

    fun addBuff(buffName: String, turnCount: Int): BuffBotOffering =
        copy(
            buffs = buffs + buffName,
            turns = turns + turnCount,
        )
}

data class BuffOfferingResult(
    val meatAmount: Long,
    val conversionMessage: String? = null,
    val abortMessage: String? = null,
)
