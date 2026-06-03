package net.sourceforge.kolmafia.data

data class ConsumableData(
    val name: String,
    val type: ConsumableType,
    val amount: Int,           // fullness/inebriety/spleenhit consumed
    val levelReq: Int,
    val quality: ConsumableQuality,
    val advMin: Int,
    val advMax: Int,
    val muscMin: Int,
    val muscMax: Int,
    val mystMin: Int,
    val mystMax: Int,
    val moxieMin: Int,
    val moxieMax: Int,
    val notes: String          // raw notes field
) {
    val advRange get() = advMin..advMax
    val isGoodOrBetter get() = quality >= ConsumableQuality.GOOD
}
