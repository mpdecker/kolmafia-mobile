package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.MayamAvailability
import net.sourceforge.kolmafia.request.AsdonMartinRequest
import net.sourceforge.kolmafia.request.CampAwayRequest
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.request.FalloutShelterRequest
import net.sourceforge.kolmafia.request.LoathingIdolRequest
import net.sourceforge.kolmafia.request.MayamRequest
import net.sourceforge.kolmafia.request.TerminalRequest

internal fun GameRuntimeLibrary.cliTerminal(parameters: String, print: (String) -> Unit) {
    val parts = parameters.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.size < 2) {
        print("Usage: terminal enhance|enquiry|educate|extrude <target>")
        return
    }
    val verb = parts[0].lowercase()
    val target = parts.drop(1).joinToString(" ")
    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }
    val counts: (Int) -> Int = { id ->
        inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
    }
    val request = TerminalRequest(
        client = client,
        campgroundRequest = CampgroundRequest(client),
        falloutShelterRequest = FalloutShelterRequest(client),
    )
    runBlocking {
        val result = when (verb) {
            "enhance" -> request.enhance(
                target = target,
                state = character?.state?.value,
                preferences = preferences,
                accessibleCount = counts,
            )
            "enquiry" -> request.enquiry(
                target = target,
                state = character?.state?.value,
                preferences = preferences,
                accessibleCount = counts,
            )
            "educate" -> request.educate(
                target = target,
                state = character?.state?.value,
                preferences = preferences,
                accessibleCount = counts,
            )
            "extrude" -> request.cliExtrude(
                target = target,
                state = character?.state?.value,
                preferences = preferences,
                accessibleCount = counts,
            )
            else -> Result.failure(
                IllegalArgumentException("$verb is not a valid terminal command."),
            )
        }
        result.onFailure { print(it.message ?: "Terminal $verb failed.") }
    }
}

internal fun GameRuntimeLibrary.cliCampaway(parameters: String, print: (String) -> Unit) {
    if (!parameters.trim().equals("cloud", ignoreCase = true)) {
        print("Usage: campaway cloud")
        return
    }
    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }
    runBlocking {
        CampAwayRequest(client)
            .takeCloudBuff(preferences, character?.state?.value)
            .onFailure { print(it.message ?: "Camp Away cloud buff failed.") }
    }
}

internal fun GameRuntimeLibrary.cliLoathingidol(parameters: String, print: (String) -> Unit) {
    val stance = LoathingIdolRequest.findStance(parameters)
    if (stance == 0) {
        print("Usage: loathingidol pop|ballad|rhyme|country")
        return
    }
    val useReq = useItemRequest ?: run {
        print("Use item request is not available.")
        return
    }
    val choice = choiceRequest ?: run {
        print("Choice request is not available.")
        return
    }
    val counts: (Int) -> Int = { id ->
        inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
    }
    runBlocking {
        LoathingIdolRequest(useReq, choice)
            .takeStance(stance, preferences, counts)
            .onFailure { print(it.message ?: "Loathing Idol buff failed.") }
    }
}

internal fun GameRuntimeLibrary.cliMayam(parameters: String, print: (String) -> Unit) {
    val trimmed = parameters.trim()
    val lower = trimmed.lowercase()
    if (trimmed.isEmpty() || lower == "list" || lower == "rings" || lower == "status") {
        val available = MayamAvailability.availableResonances(preferences)
        if (available.isEmpty()) {
            print("No Mayam resonances available.")
        } else {
            available.forEach { print(it) }
        }
        return
    }
    val query = MayamRequest.parseResonanceQuery(parameters)
    if (query == null) {
        print("Usage: mayam [list|rings|resonance <name>]")
        return
    }
    val useReq = useItemRequest ?: run {
        print("Use item request is not available.")
        return
    }
    val choice = choiceRequest ?: run {
        print("Choice request is not available.")
        return
    }
    val counts: (Int) -> Int = { id ->
        inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
    }
    val calendarName = gameDatabase?.item(MayamRequest.CALENDAR_ID)?.name
    val equipped = character?.state?.value?.let { state ->
        listOf(EquipmentSlot.ACC1, EquipmentSlot.ACC2, EquipmentSlot.ACC3)
            .any { slot ->
                val name = state.equippedItem(slot).orEmpty()
                if (calendarName != null && name.equals(calendarName, ignoreCase = true)) true
                else name.contains("Mayam", ignoreCase = true)
            }
    } == true
    runBlocking {
        MayamRequest(useReq, choice)
            .takeResonance(
                resonanceQuery = query,
                preferences = preferences,
                inventoryCounts = counts,
                calendarEquipped = equipped,
            )
            .onFailure { print(it.message ?: "Mayam resonance failed.") }
    }
}

internal fun GameRuntimeLibrary.cliAsdonmartin(parameters: String, print: (String) -> Unit) {
    val trimmed = parameters.trim()
    val lower = trimmed.lowercase()
    val parts = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
    when {
        trimmed.isEmpty() || lower == "status" -> {
            val fuel = CampgroundItemSync.asdonMartinFuel(preferences)
            val styleId = currentAsdonDriveStyle()
            val style = AsdonMartinRequest.driveStyleName(styleId) ?: "none"
            print("Asdon Martin fuel: $fuel")
            print("Drive style: $style")
        }
        lower == "clear" || lower == "drive clear" -> {
            val styleId = currentAsdonDriveStyle()
            if (styleId !in 0..8) {
                print("You are not currently driving.")
                return
            }
            val client = httpClient ?: run {
                print("HTTP client is not available.")
                return
            }
            runBlocking {
                AsdonMartinRequest(client)
                    .clearDrive(styleId)
                    .onFailure { print(it.message ?: "Asdon Martin clear failed.") }
            }
        }
        parts.firstOrNull().equals("fuel", ignoreCase = true) -> {
            val rest = parts.drop(1).joinToString(" ")
            val (qty, name) = parseConsumeQtyName(rest) ?: run {
                print("Usage: asdonmartin fuel [N] <item>")
                return
            }
            val itemId = gameDatabase?.item(name)?.id ?: run {
                print("$name cannot be used as fuel.")
                return
            }
            if (!retrieveForCliConsume(itemId, qty, print)) return
            val client = httpClient ?: run {
                print("HTTP client is not available.")
                return
            }
            runBlocking {
                AsdonMartinRequest(client)
                    .fuel(itemId, qty)
                    .onFailure { print(it.message ?: "Asdon Martin fuel failed.") }
            }
        }
        else -> {
            val styleId = AsdonMartinRequest.parseDriveStyle(parameters)
            if (styleId < 0) {
                print("Usage: asdonmartin [status|clear|drive <style>|fuel [N] item]")
                return
            }
            val client = httpClient ?: run {
                print("HTTP client is not available.")
                return
            }
            val currentStyle = currentAsdonDriveStyle()
            runBlocking {
                AsdonMartinRequest(client)
                    .drive(styleId, preferences, currentStyle)
                    .onFailure { print(it.message ?: "Asdon Martin drive failed.") }
            }
        }
    }
}

private fun GameRuntimeLibrary.currentAsdonDriveStyle(): Int {
    val effects = effectManager?.state?.value?.effects.orEmpty()
    for (id in 0..8) {
        val name = AsdonMartinRequest.driveStyleName(id) ?: continue
        if (effects.any { it.name.equals(name, ignoreCase = true) }) return id
    }
    return -1
}
