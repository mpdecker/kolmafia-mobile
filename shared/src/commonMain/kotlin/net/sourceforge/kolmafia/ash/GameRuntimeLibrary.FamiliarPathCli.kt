package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.quest.CandyDevilerChoiceSync
import net.sourceforge.kolmafia.session.BugbearManager
import net.sourceforge.kolmafia.session.GreyYouManager

/**
 * Phases 1023–1032 — Familiar / path CLI Track B:
 * familiar lock|unlock, absorptions, gooskills, bugbears, chibi, panda,
 * devilcandyegg, train.
 */
private const val CANDY_EGG_DEVILER_ID = 11774

private val PANDA_COMEDY = setOf("insult", "observe", "prop")
private val PANDA_BAND = mapOf(
    "bognort" to "Bognort",
    "guitarist" to "Bognort",
    "stinkface" to "Stinkface",
    "vocalist" to "Stinkface",
    "flargwurm" to "Flargwurm",
    "bassist" to "Flargwurm",
    "jim" to "Jim",
    "drummer" to "Jim",
)

/** Desktop FamiliarCommand lock|unlock → familiar.php?action=lockequip (AshP903). */
internal fun GameRuntimeLibrary.cliFamiliarEquipmentLock(lock: Boolean, rt: AshRuntimeContext) {
    val prefs = preferences
    val current = prefs?.getBoolean("familiarEquipmentLocked", false) ?: false
    if (lock && current) {
        rt.print("Familiar item already locked.")
        return
    }
    if (!lock && !current) {
        rt.print("Familiar item already unlocked.")
        return
    }
    visitKolPage("familiar.php?action=lockequip")
    prefs?.setBoolean("familiarEquipmentLocked", lock)
    rt.print(if (lock) "Familiar item locked." else "Familiar item unlocked.")
}

/** Desktop AbsorptionsCommand — alias of greyyou status dump. */
internal fun GameRuntimeLibrary.runAbsorptionsCli(parameters: String, rt: AshRuntimeContext) {
    runGreyYouCli(parameters, rt)
}

/** Desktop GooSkillsCommand — list Grey You goo skills. */
internal fun GameRuntimeLibrary.runGooSkillsCli(parameters: String, rt: AshRuntimeContext) {
    val filter = parameters.trim().lowercase()
    val neededOnly = filter.split(Regex("\\s+")).any { it == "needed" }
    GreyYouManager.loadRegistry()
    val rows = GreyYouManager.allGooSkills.values
        .filter { !neededOnly || !GreyYouManager.haveLearned(it.skillId ?: 0) }
        .filter {
            filter.isBlank() ||
                filter == "all" ||
                filter == "needed" ||
                it.skillName.orEmpty().lowercase().contains(filter) ||
                it.monsterName.lowercase().contains(filter)
        }
        .sortedBy { it.skillId ?: 0 }
    rows.forEach {
        val known = it.skillId?.let { id -> GreyYouManager.haveLearned(id) } == true
        val status = if (known) "[x]" else "[ ]"
        val effect = it.enchantments.ifBlank { it.type.name }
        rt.print("$status ${it.skillName}: ${it.monsterName} — $effect")
    }
    if (rows.isEmpty()) rt.print("No Grey You goo skills matched.")
}

/** Desktop BugbearsCommand — mothership biodata status dump. */
internal fun GameRuntimeLibrary.runBugbearsCli(rt: AshRuntimeContext) {
    val prefs = preferences
    for (data in BugbearManager.BUGBEAR_DATA) {
        val raw = prefs?.getString(data.status, "").orEmpty().ifBlank { "0" }
        val value = raw.toIntOrNull()?.let { "$it/${data.level * 3}" } ?: raw
        val zones = data.zones.joinToString(" / ")
        rt.print("${data.shipZone}: $value — ${data.bugbear} ($zones)")
    }
}

/** Desktop ChibiBuddyCommand — pref status when recorded; else usage. */
internal fun GameRuntimeLibrary.runChibiCli(parameters: String, rt: AshRuntimeContext) {
    val prefs = preferences
    val name = prefs?.getString("chibiName", "").orEmpty()
    val hasStats = listOf("chibiFitness", "chibiIntelligence", "chibiSocialization", "chibiAlignment")
        .any { (prefs?.getInt(it, 0) ?: 0) > 0 }
    if (name.isBlank() && !hasStats) {
        rt.print("Usage: chibi [chat]")
        rt.print("No ChibiBuddy™ status recorded. Visit your ChibiBuddy to sync prefs.")
        return
    }
    if (parameters.trim().equals("chat", ignoreCase = true)) {
        if (prefs?.getBoolean("_chibiChanged", false) == true) {
            rt.print("You've already chatted with your ChibiBuddy™ today")
            return
        }
        rt.print("ChibiBuddy chat is not available in this build.")
        return
    }
    if (parameters.isNotBlank()) {
        rt.print("Usage: chibi [chat]")
        return
    }
    val displayName = name.ifBlank { "(unnamed)" }
    val fitness = prefs?.getInt("chibiFitness", 0) ?: 0
    val intelligence = prefs?.getInt("chibiIntelligence", 0) ?: 0
    val socialization = prefs?.getInt("chibiSocialization", 0) ?: 0
    val alignment = prefs?.getInt("chibiAlignment", 0) ?: 0
    rt.print("ChibiBuddy™ $displayName")
    rt.print("Fitness: $fitness/10  Intelligence: $intelligence/10")
    rt.print("Socialization: $socialization/10  Alignment: $alignment/10")
}

/**
 * Desktop PandaCommand — thin pandamonium.php HTTP ([PandamoniumVisitSync] visit hooks apply).
 */
internal fun GameRuntimeLibrary.runPandaCli(parameters: String, rt: AshRuntimeContext) {
    val trimmed = parameters.trim()
    if (trimmed.isEmpty()) {
        rt.print("Usage: panda moan | temple | comedy <type> | arena <bandmember> <item>")
        return
    }
    val moanTemple = Regex("""^(moan|temple)$""", RegexOption.IGNORE_CASE).matchEntire(trimmed)
    if (moanTemple != null) {
        val action = if (moanTemple.groupValues[1].equals("moan", ignoreCase = true)) "moan" else "temp"
        visitKolPage("pandamonium.php?action=$action", applyQuestHooks = true)
        return
    }
    val comedy = Regex("""^comedy\s+(\S+)$""", RegexOption.IGNORE_CASE).matchEntire(trimmed)
    if (comedy != null) {
        val type = comedy.groupValues[1].lowercase()
        if (type !in PANDA_COMEDY) {
            rt.print("What kind of comedy is \"$type\"?")
            return
        }
        visitKolPage("pandamonium.php?action=mourn&preaction=$type", applyQuestHooks = true)
        return
    }
    val arena = Regex("""^arena\s+(\S+)\s+(.+)$""", RegexOption.IGNORE_CASE).matchEntire(trimmed)
    if (arena != null) {
        val member = PANDA_BAND[arena.groupValues[1].lowercase()]
        if (member == null) {
            rt.print("I don't think \"${arena.groupValues[1]}\" is a member of the band.")
            return
        }
        val itemQuery = arena.groupValues[2].trim()
        val itemId = itemQuery.toIntOrNull()
            ?: ItemDatabase.getByName(itemQuery)?.id
            ?: gameDatabase?.item(itemQuery)?.id
        if (itemId == null || itemId <= 0) {
            rt.print("WHAT did you want to give to $member?")
            return
        }
        visitKolPage(
            "pandamonium.php?action=sven&bandmember=$member&togive=$itemId&preaction=try",
            applyQuestHooks = true,
        )
        return
    }
    rt.print("Usage: panda moan | temple | comedy <type> | arena <bandmember> <item>")
}

/** Desktop DevilCandyEggCommand — inventory.php?action=eggdevil + choice 1544. */
internal fun GameRuntimeLibrary.runDevilCandyEggCli(parameters: String, rt: AshRuntimeContext) {
    val qty = inventoryManager?.state?.value?.items?.get(CANDY_EGG_DEVILER_ID)?.quantity ?: 0
    if (qty <= 0) {
        rt.print("You don't have a candy egg deviler.")
        return
    }
    val query = parameters.trim()
    if (query.isEmpty()) {
        rt.print("Usage: devilcandyegg <item>")
        return
    }
    val itemId = query.toIntOrNull()
        ?: ItemDatabase.getByName(query)?.id
        ?: gameDatabase?.item(query)?.id
    if (itemId == null || itemId <= 0) {
        rt.print("Unknown item: $query")
        return
    }
    if (!ItemDatabase.isCandyItem(itemId)) {
        rt.print("You can only devil candy.")
        return
    }
    visitKolPage("inventory.php?action=eggdevil")
    visitKolPage("choice.php?whichchoice=${CandyDevilerChoiceSync.CHOICE_ID}&option=1&a=$itemId")
}

/** Desktop TrainFamiliarCommand — mobile has no training UI; status stub. */
internal fun GameRuntimeLibrary.runTrainFamiliarCli(parameters: String, rt: AshRuntimeContext) {
    val weight = familiarManager?.state?.value?.activeFamiliar?.weight
        ?: character?.state?.value?.familiarWeight
        ?: 0
    val race = familiarManager?.state?.value?.activeFamiliar?.race
        ?: character?.state?.value?.familiarName
        ?: "none"
    rt.print("Familiar training UI is not available in this build.")
    rt.print("Current familiar: $race (weight $weight)")
    if (parameters.isNotBlank()) {
        rt.print("Usage: train base <weight> | buffed <weight> | turns <number> (desktop only)")
    }
}
