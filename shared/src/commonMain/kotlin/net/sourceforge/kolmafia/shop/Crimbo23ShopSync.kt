package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop Crimbo23*Request token balance sync from shop visit HTML. */
object Crimbo23ShopSync {

    private val SHOP_ID_PATTERN = Regex("""whichshop=([^&]+)""", RegexOption.IGNORE_CASE)

    fun applyVisitShop(
        html: String,
        url: String?,
        prefs: Preferences?,
        sessionLogger: SessionLogger?,
        state: CharacterState?,
    ) {
        if (prefs == null) return
        if (url?.contains("action=buy", ignoreCase = true) == true) return
        val shopId = url?.let { SHOP_ID_PATTERN.find(it)?.groupValues?.getOrNull(1) } ?: return
        syncFromShopHtml(html, shopId, prefs)
    }

    const val ELF_MPC = 11408
    const val PIECE_OF_12 = 11409
    const val MACHINE_PARTS = 11402
    const val FLOTSAM = 11405

    const val AVAILABLE_ELF_MPC_PREF = "availableCrimbo23ElfMpc"
    const val AVAILABLE_PIECE_OF_12_PREF = "availableCrimbo23PieceOf12"
    const val AVAILABLE_MACHINE_PARTS_PREF = "availableCrimbo23MachineParts"
    const val AVAILABLE_FLOTSAM_PREF = "availableCrimbo23Flotsam"

    private val ELF_MPC_PATTERN =
        Regex("""([\d,]+) Elf Guard MPCs""", RegexOption.IGNORE_CASE)
    private val PIECE_OF_12_PATTERN =
        Regex("""([\d,]+) Crimbuccaneer pieces? of 12""", RegexOption.IGNORE_CASE)
    private val MACHINE_PARTS_PATTERN =
        Regex("""<td>([\d,]+) piles? of Elf Army machine parts</td>""", RegexOption.IGNORE_CASE)
    private val FLOTSAM_PATTERN =
        Regex("""<td>([\d,]+) piles? of Crimbuccaneer flotsam</td>""", RegexOption.IGNORE_CASE)

    fun syncFromShopHtml(html: String, shopId: String, prefs: Preferences) {
        when {
            shopId.endsWith("_elf_armory", ignoreCase = true) ->
                syncTokenBalance(
                    html = html,
                    prefs = prefs,
                    prefKey = AVAILABLE_MACHINE_PARTS_PREF,
                    pattern = MACHINE_PARTS_PATTERN,
                    zeroTest = "no piles of Elf Army machine parts",
                )
            shopId.endsWith("_pirate_armory", ignoreCase = true) ->
                syncTokenBalance(
                    html = html,
                    prefs = prefs,
                    prefKey = AVAILABLE_FLOTSAM_PREF,
                    pattern = FLOTSAM_PATTERN,
                    zeroTest = "no piles of Crimbuccaneer flotsam",
                )
            shopId.contains("_elf_", ignoreCase = true) ->
                syncTokenBalance(
                    html = html,
                    prefs = prefs,
                    prefKey = AVAILABLE_ELF_MPC_PREF,
                    pattern = ELF_MPC_PATTERN,
                    zeroTest = "no Elf Guard MPCs",
                )
            shopId.contains("_pirate_", ignoreCase = true) ->
                syncTokenBalance(
                    html = html,
                    prefs = prefs,
                    prefKey = AVAILABLE_PIECE_OF_12_PREF,
                    pattern = PIECE_OF_12_PATTERN,
                    zeroTest = "no Crimbuccaneer pieces of 12",
                )
        }
    }

    private fun syncTokenBalance(
        html: String,
        prefs: Preferences,
        prefKey: String,
        pattern: Regex,
        zeroTest: String,
    ) {
        if (html.contains(zeroTest, ignoreCase = true)) {
            prefs.setInt(prefKey, 0)
            return
        }
        val count = pattern.find(html)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: return
        prefs.setInt(prefKey, count)
    }
}
