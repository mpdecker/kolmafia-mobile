package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.http.KOL_BASE_URL

data class GiftPackage(
    val name: String,
    val radio: Int,
    val maxCapacity: Int,
    val materialCost: Int,
)

object GiftPackages {
    val ALL = listOf(
        GiftPackage("plain brown wrapper", 1, 1, 50),
        GiftPackage("less-than-three-shaped box", 2, 2, 100),
        GiftPackage("exactly-three-shaped box", 3, 3, 200),
        GiftPackage("chocolate box", 4, 4, 500),
        GiftPackage("miniature coffin", 5, 5, 1000),
        GiftPackage("solid asbestos box", 6, 6, 2000),
        GiftPackage("solid linoleum box", 7, 7, 5000),
        GiftPackage("solid chrome box", 8, 8, 10000),
        GiftPackage("cryptic puzzle box", 9, 9, 20000),
        GiftPackage("refrigerated biohazard container", 10, 10, 50000),
        GiftPackage("magnetic field", 11, 11, 100000),
    )

    fun smallestFor(itemCount: Int): GiftPackage? =
        ALL.firstOrNull { it.maxCapacity >= itemCount.coerceAtLeast(1) }
}

open class SendGiftRequest(private val client: HttpClient) {

    open suspend fun send(
        recipient: String,
        message: String,
        attachments: List<MailAttachment>,
        fromStorage: Boolean = false,
    ): Result<Unit> {
        if (attachments.isEmpty()) {
            return Result.failure(Exception("Gift requires at least one item"))
        }
        val pkg = GiftPackages.smallestFor(attachments.size)
            ?: return Result.failure(Exception("No gift package large enough"))
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/town_sendgift.php",
                formParameters = parameters {
                    append("action", "Yep.")
                    append("towho", recipient)
                    append("note", message)
                    append("insidenote", message)
                    append("whichpackage", pkg.radio.toString())
                    append("fromwhere", if (fromStorage) "1" else "0")
                    attachments.forEachIndexed { index, att ->
                        val n = index + 1
                        val itemField = if (fromStorage) "hagnks_whichitem$n" else "whichitem$n"
                        val qtyField = if (fromStorage) "hagnks_howmany$n" else "howmany$n"
                        append(itemField, att.itemId.toString())
                        append(qtyField, att.quantity.toString())
                    }
                },
            )
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("HTTP ${response.status.value}"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
