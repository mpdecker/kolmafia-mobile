package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.combat.CombatActionManager
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.mall.MallPriceDatabase
import net.sourceforge.kolmafia.platform.UserDataFileIO
import net.sourceforge.kolmafia.request.ClanFortuneRequest

/**
 * CLI unnamed leftovers mega (7751–7810) — enable/disable, login, neweffect, update,
 * condition check, ccs load, fortune consult, goal aliases, help inventory.
 */

internal fun GameRuntimeLibrary.runEnableDisableCli(
    enable: Boolean,
    parameters: String,
    rt: AshRuntimeContext,
) {
    val raw = parameters.trim().lowercase()
    if (raw.isEmpty()) {
        rt.print(if (enable) "enable all | <command> [, <command>]..." else "disable <command> [, <command>]...")
        return
    }
    if (enable && (raw == "all" || raw == "*")) {
        disabledFeatures.clear()
        rt.print("All CLI commands enabled.")
        return
    }
    val names = raw.split(Regex("[,\\s]+")).filter { it.isNotEmpty() }
    for (name in names) {
        if (enable) {
            disabledFeatures.remove(name)
            rt.print("Enabled: $name")
        } else {
            disabledFeatures.add(name)
            rt.print("Disabled: $name")
        }
    }
}

internal fun GameRuntimeLibrary.isCliCommandDisabled(command: String): Boolean {
    val name = command.lowercase().removeSuffix("?")
    if (name == "enable" || name == "disable") return false
    return "all" in disabledFeatures || name in disabledFeatures
}

internal fun GameRuntimeLibrary.runNewEffectCli(parameters: String, rt: AshRuntimeContext) {
    val descId = parameters.trim()
    if (descId.isEmpty()) {
        rt.print("neweffect <effect description ID>")
        return
    }
    val existing = EffectDatabase.getByDescId(descId)
    if (existing != null) {
        rt.print("Already known: ${existing.name} (id=${existing.id})")
        return
    }
    // Prefetch desc_effect so the runtime can learn the name when KoL is reachable.
    val html = visitKolPage("desc_effect.php?whicheffect=$descId").orEmpty()
    val name = Regex("""<font[^>]*size=\+1[^>]*>\s*([^<]+)""", RegexOption.IGNORE_CASE)
        .find(html)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        .ifBlank {
            Regex("""<b>([^<]+)</b>""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        }
    if (name.isNotEmpty()) {
        val id = EffectDatabase.registerRuntimeEffect(name, descId)
        rt.print("Learned effect: $name (id=$id, descid=$descId)")
    } else {
        rt.print("Could not learn effect for descid=$descId")
    }
}

internal fun GameRuntimeLibrary.runUpdateDataCli(parameters: String, rt: AshRuntimeContext) {
    val raw = parameters.trim()
    when {
        raw.equals("clear", ignoreCase = true) -> {
            // Desktop deletes adventure override files; mobile clears user location override prefs.
            preferences?.setString("adventureOverride", "")
            UserDataFileIO.writeText("adventures.txt", "")
            rt.print("Adventure data overrides cleared.")
        }
        raw.equals("save", ignoreCase = true) -> {
            MallPriceDatabase.save()
            rt.print("Saved mallprices.txt override.")
        }
        raw.startsWith("prices", ignoreCase = true) -> {
            val source = raw.removePrefix("prices").removePrefix("PRICES").trim()
            if (source.isEmpty()) {
                rt.print("update prices <URL or filename>")
                return
            }
            val text = when {
                source.startsWith("http://", ignoreCase = true) ||
                    source.startsWith("https://", ignoreCase = true) ->
                    visitKolPage(source) // may fail for non-KoL hosts; still best-effort
                else -> UserDataFileIO.readText(source)
            }
            val count = MallPriceDatabase.load(text)
            rt.print("Loaded $count mall price rows from $source")
        }
        else -> rt.print(
            "\"update\" doesn't do what you think it does. Use: update clear | save | prices <URL or filename>",
        )
    }
}

internal fun GameRuntimeLibrary.runLoginCli(parameters: String, rt: AshRuntimeContext) {
    val username = parameters.trim()
    if (username.isEmpty()) {
        rt.print("login <username>")
        return
    }
    val prefs = preferences ?: run {
        rt.print("Preferences are not available.")
        return
    }
    val password = prefs.getString("saveState.$username", "").ifBlank {
        prefs.getString("saveState.${username.lowercase()}", "")
    }
    if (password.isEmpty()) {
        rt.print("No stored password for user: $username")
        rt.print("Logout and enter credentials manually.")
        return
    }
    sessionManager?.logout()
    val manager = sessionManager
    if (manager == null) {
        rt.print("Session manager is not available.")
        return
    }
    runBlocking {
        try {
            manager.login(username, password)
            rt.print("Logged in as $username")
        } catch (e: Exception) {
            rt.print(e.message ?: "Login failed for $username")
        }
    }
}

internal fun GameRuntimeLibrary.runTimeoutCli(rt: AshRuntimeContext) {
    // Desktop TimeoutCommand: logout leaving character/prefs intact (same as LogoutRequest).
    sessionManager?.logout()
    rt.print("Timed out (logged out; local state retained).")
}

internal fun GameRuntimeLibrary.runConditionCheckCli(rt: AshRuntimeContext) {
    val manager = goalManager
    if (manager == null || !manager.hasAnyGoals()) {
        rt.print("No conditions to check.")
        return
    }
    val inventory = inventoryManager
    manager.allGoalsAsStrings().forEach { rt.print(it) }
    val remaining = manager.remainingItemGoalCount { itemId ->
        inventory?.getCount(itemId) ?: 0
    }
    rt.print("Conditions list validated against available items. Remaining item goals: $remaining")
}

internal fun GameRuntimeLibrary.runCcsSetCli(name: String, rt: AshRuntimeContext) {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) {
        runCcsStatusCli(rt)
        return
    }
    assignCombatScript(trimmed)
    val loaded = CombatActionManager.loadStrategyLookup(trimmed, preferences)
    if (loaded) {
        rt.print("CCS set to $trimmed")
    } else {
        rt.print("CCS name rejected: $trimmed")
    }
}

internal fun GameRuntimeLibrary.runFortuneConsultCli(
    parameters: String,
    print: (String) -> Unit,
) {
    val parts = parameters.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) {
        print("What do you want to request from the clan fortune teller?")
        return
    }
    val prefs = preferences ?: run {
        print("Preferences are not available.")
        return
    }
    if (prefs.getInt("_clanFortuneConsultUses", 0) >= 3) {
        print("You already consulted with a clanmate 3 times today.")
        return
    }
    val lounge = clanLoungeRequest ?: run {
        print("Clan lounge request is not available.")
        return
    }
    val choice = choiceRequest ?: run {
        print("Choice request is not available.")
        return
    }
    val (player, w1, w2, w3) = when (parts.size) {
        4 -> listOf(parts[0], parts[1], parts[2], parts[3])
        else -> listOf(
            parameters.trim(),
            prefs.getString(ClanFortuneRequest.WORD1_PREF, ""),
            prefs.getString(ClanFortuneRequest.WORD2_PREF, ""),
            prefs.getString(ClanFortuneRequest.WORD3_PREF, ""),
        )
    }
    runBlocking {
        ClanFortuneRequest(lounge, choice)
            .consultClanmate(player, prefs, w1, w2, w3)
            .onSuccess { print("Consulted Madame Zatara about $player.") }
            .onFailure { print(it.message ?: "Fortune consult failed.") }
    }
}
