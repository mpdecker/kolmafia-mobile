package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [AsdonMartinCommand] drive path — campground.php preaction=drive. */
class AsdonMartinRequest(
    private val client: HttpClient,
) {
    suspend fun drive(
        styleId: Int,
        preferences: Preferences?,
        currentStyleId: Int = -1,
    ): Result<String> {
        if (styleId !in 0..8) {
            return Result.failure(IllegalArgumentException("Driving style not recognised"))
        }
        if (!CampgroundItemSync.hasWorkshedItem(preferences, CampgroundItemSync.ASDON_MARTIN_ID)) {
            return Result.failure(IllegalStateException("You do not have an Asdon Martin"))
        }
        val fuel = CampgroundItemSync.asdonMartinFuel(preferences)
        if (fuel < MIN_FUEL) {
            return Result.failure(IllegalStateException("You haven't got enough fuel"))
        }
        return try {
            if (currentStyleId in 0..8 && currentStyleId != styleId) {
                undrive(currentStyleId).exceptionOrNull()?.let { return Result.failure(it) }
            }
            val response = client.submitForm(
                url = "$KOL_BASE_URL/campground.php",
                formParameters = driveForm(styleId),
            )
            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Asdon Martin drive failed."))
            }
            Result.success(response.bodyAsText())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun undrive(styleId: Int): Result<String> = try {
        val name = driveStyleName(styleId) ?: return Result.failure(
            IllegalArgumentException("Unknown drive style"),
        )
        val response = client.submitForm(
            url = "$KOL_BASE_URL/campground.php",
            formParameters = parameters {
                append("preaction", "undrive")
                append("stop", "Stop Driving $name")
            },
        )
        if (!response.status.isSuccess()) {
            Result.failure(IllegalStateException("Asdon Martin undrive failed."))
        } else {
            Result.success(response.bodyAsText())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        const val MIN_FUEL = 37

        private val DRIVE_STYLES = listOf(
            "Obnoxiously",
            "Stealthily",
            "Wastefully",
            "Safely",
            "Recklessly",
            "Quickly",
            "Intimidatingly",
            "Observantly",
            "Waterproofly",
        )

        fun findDriveStyle(name: String): Int {
            val n = name.trim()
            if (n.isEmpty()) return -1
            return DRIVE_STYLES.indexOfFirst { it.equals(n, ignoreCase = true) }
        }

        fun driveStyleName(index: Int): String? =
            DRIVE_STYLES.getOrNull(index)

        /** Parse `drive <style>` from CLI parameters. */
        fun parseDriveStyle(parameters: String): Int {
            val parts = parameters.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (parts.isEmpty()) return -1
            val style = if (parts[0].equals("drive", ignoreCase = true)) {
                parts.drop(1).joinToString(" ")
            } else {
                parts.joinToString(" ")
            }
            return findDriveStyle(style)
        }

        internal fun driveForm(styleId: Int) = parameters {
            append("preaction", "drive")
            append("whichdrive", styleId.toString())
        }
    }
}
