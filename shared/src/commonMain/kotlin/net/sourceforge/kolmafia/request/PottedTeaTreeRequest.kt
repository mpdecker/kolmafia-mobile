package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.campground.CampgroundInventorySync
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.TeaTreeChoiceSync
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.session.SessionLogger

/** Typed request for harvesting the potted tea tree via campground and choices 1104/1105. */
open class PottedTeaTreeRequest(
    private val client: HttpClient,
    private val campgroundRequest: CampgroundRequest,
    private val choiceRequest: ChoiceRequest,
    private val inventoryManager: InventoryManager? = null,
    private val preferences: Preferences? = null,
    private val sessionLogger: SessionLogger? = null,
) {
    private val handledSignatures = mutableSetOf<Pair<String, String>>()

    open suspend fun shake(): Result<String> = harvest(teaItemId = null)

    open suspend fun select(teaItemId: Int): Result<String> {
        if (teaItemId <= 0) {
            return Result.failure(IllegalArgumentException("Tea item ID must be positive."))
        }
        return harvest(teaItemId)
    }

    fun parseResponse(
        url: String,
        html: String,
        teaItemId: Int? = null,
    ): Boolean {
        val signature = url to html
        if (signature in handledSignatures) return true
        if (!isTeaTreeSuccess(html)) return false
        val choiceId = extractChoiceId(url) ?: return false
        val decision = extractChoiceDecision(url)
        val choiceUrl = if (teaItemId != null && teaItemId > 0 && !url.contains("itemid", ignoreCase = true)) {
            buildString {
                append(url)
                append(if (url.contains('?')) '&' else '?')
                append("itemid=$teaItemId")
            }
        } else {
            url
        }
        val applied = TeaTreeChoiceSync.apply(
            choiceId = choiceId,
            decision = decision,
            preferences = preferences,
            choiceUrl = choiceUrl,
            html = html,
        )
        if (!applied) return false
        ITEM_REL.findAll(html).forEach { match ->
            val itemId = match.groupValues[1].toIntOrNull()
            val quantity = match.groupValues[2].toIntOrNull()
            if (itemId != null && itemId > 0 && quantity != null && quantity > 0) {
                ResultProcessor.processItem(
                    itemId,
                    quantity,
                    preferences = preferences,
                    inventory = inventoryManager,
                )
            }
        }
        handledSignatures += signature
        return true
    }

    private suspend fun harvest(teaItemId: Int?): Result<String> {
        if (preferences?.getBoolean("_pottedTeaTreeUsed", false) == true) {
            return Result.failure(
                IllegalStateException("You have already harvested tea from your potted tea tree today."),
            )
        }
        if (!hasPottedTeaTree()) {
            return Result.failure(IllegalStateException("You don't have a potted tea tree."))
        }
        return try {
            campgroundRequest.visitAction("teatree").getOrElse { return Result.failure(it) }
            val html = if (teaItemId == null) {
                val choiceResult = choiceRequest.choose(TeaTreeChoiceSync.TREE_TEA, SHAKE_OPTION)
                val (choiceHtml, url) = choiceResult.getOrElse { return Result.failure(it) }
                val responseUrl = choiceUrl(url, TeaTreeChoiceSync.TREE_TEA, SHAKE_OPTION)
                if (!parseResponse(responseUrl, choiceHtml)) {
                    return Result.failure(
                        IllegalStateException("Tea tree shake response was not successful."),
                    )
                }
                sessionLogger?.appendRawLine("teatree shake")
                choiceHtml
            } else {
                choiceRequest.choose(TeaTreeChoiceSync.TREE_TEA, PICK_OPTION)
                    .getOrElse { return Result.failure(it) }
                val choiceResult = choiceRequest.choose(
                    TeaTreeChoiceSync.SPECIFICI_TEA,
                    SHAKE_OPTION,
                    mapOf("itemid" to teaItemId.toString()),
                )
                val (choiceHtml, url) = choiceResult.getOrElse { return Result.failure(it) }
                val responseUrl = choiceUrl(
                    url,
                    TeaTreeChoiceSync.SPECIFICI_TEA,
                    SHAKE_OPTION,
                    "itemid" to teaItemId.toString(),
                )
                if (!parseResponse(responseUrl, choiceHtml, teaItemId)) {
                    return Result.failure(
                        IllegalStateException("Tea tree harvest response was not successful."),
                    )
                }
                val teaName = ItemDatabase.getItemName(teaItemId).ifEmpty { "item #$teaItemId" }
                sessionLogger?.appendRawLine("teatree $teaName")
                choiceHtml
            }
            Result.success(html)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun hasPottedTeaTree(): Boolean =
        (CampgroundInventorySync.load(preferences)[POTTED_TEA_TREE_ID] ?: 0) > 0 ||
            (inventoryManager?.getCount(POTTED_TEA_TREE_ID) ?: 0) > 0

    private fun choiceUrl(
        url: String,
        choiceId: Int,
        option: Int,
        extra: Pair<String, String>? = null,
    ): String = buildString {
        append(url)
        append(if (url.contains('?')) '&' else '?')
        append("whichchoice=$choiceId")
        append("&option=$option")
        if (extra != null) {
            append("&${extra.first}=${extra.second}")
        }
    }

    private fun extractChoiceId(url: String): Int? =
        WHICH_CHOICE.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()

    private fun extractChoiceDecision(url: String): Int =
        OPTION.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0

    private fun isTeaTreeSuccess(html: String): Boolean =
        html.contains("You acquire an item", ignoreCase = true)

    companion object {
        const val POTTED_TEA_TREE_ID = 8600
        private const val SHAKE_OPTION = 1
        private const val PICK_OPTION = 2
        private val WHICH_CHOICE = Regex("""(?:^|[?&])whichchoice=(\d+)""", RegexOption.IGNORE_CASE)
        private val OPTION = Regex("""(?:^|[?&])(?:option|decision)=(\d+)""", RegexOption.IGNORE_CASE)
        private val ITEM_REL = Regex(
            """rel=["'][^"']*\bid=(\d+)[^"']*\bn=(\d+)[^"']*["']""",
            RegexOption.IGNORE_CASE,
        )
    }
}
