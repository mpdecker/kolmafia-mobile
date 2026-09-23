package net.sourceforge.kolmafia.webui

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [UseLinkDecorator] deepen — acquire-item / fight / choice use·equip·create links
 * with Maximizer Speculation hover tooltips on use/equip labels.
 */
object UseLinkDecorator {
    private val acquireItem = Regex(
        """You acquire an item:\s*<b>([^<]+)</b>""",
        RegexOption.IGNORE_CASE,
    )
    private val madeItem = Regex(
        """O hai, I made dis:\s*<b>([^<]+)</b>""",
        RegexOption.IGNORE_CASE,
    )
    private val itemTableRel = Regex(
        """<table[^>]*class=["']item["'][^>]*rel=["']id=(\d+)[^"']*["'][^>]*>.*?</table>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private val deferred = StringBuilder()

    fun decorate(
        location: String,
        buffer: StringBuilder,
        preferences: Preferences? = null,
    ) {
        if (location.startsWith("afterlife.php")) return
        if (preferences?.getBoolean("relayAddsUseLinks", true) == false) return
        if ((location.startsWith("mallstore.php") || location.startsWith("backoffice.php")) &&
            preferences?.getBoolean("canInteract", true) == false
        ) {
            return
        }

        val inCombat = location.startsWith("fight.php")
        val inChoice = location.startsWith("choice.php")
        val inInventory = location.startsWith("inventory.php") ||
            location.startsWith("inv_use.php") ||
            location.startsWith("craft.php") ||
            location.startsWith("storage.php")
        val text = buffer.toString()
        if (!inCombat && !inChoice && !inInventory &&
            !text.contains("You acquire", ignoreCase = true) &&
            !text.contains("O hai, I made dis", ignoreCase = true)
        ) {
            return
        }

        val duringCombat = inCombat && (
            text.contains("fight.php") &&
                !text.contains("Adventure Again", ignoreCase = true) &&
                !text.contains("You win the fight", ignoreCase = true)
            )
        val duringChoice = inChoice && text.contains("choice.php") &&
            !text.contains("Adventure Again", ignoreCase = true)

        if (duringCombat || duringChoice) {
            // Defer: collect links for later; leave page text unchanged for now.
            collectDeferred(text, preferences)
            return
        }

        var updated = text
        updated = annotateAcquires(updated, preferences)
        updated = annotateItemTables(updated, preferences)
        if (inInventory) {
            updated = annotateInventoryBoldItems(updated, preferences)
        }

        if (inCombat || inChoice) {
            val pos = updated.lastIndexOf("</table>")
            if (pos >= 0 && deferred.isNotEmpty()) {
                val tag = if (inCombat) "Found in this fight" else "Previously seen"
                val insert = buildString {
                    append("</table><table><tr><td colspan=2>")
                    append(tag)
                    append(":</td></tr>")
                    append(deferred)
                }
                deferred.setLength(0)
                updated = updated.substring(0, pos) + insert + updated.substring(pos)
            }
        }

        if (updated != text) {
            buffer.setLength(0)
            buffer.append(updated)
        }
    }

    private fun collectDeferred(text: String, preferences: Preferences?) {
        for (m in acquireItem.findAll(text)) {
            val name = m.groupValues[1]
            val links = linkMarkup(name, ItemDatabase.getByName(name)?.id ?: 0, preferences)
            if (links.isNotEmpty()) {
                deferred.append("<tr><td>").append(name).append("</td><td>")
                    .append(links).append("</td></tr>")
            }
        }
    }

    private fun annotateInventoryBoldItems(text: String, preferences: Preferences?): String {
        if (text.contains("mafia-uselink")) return text
        val bold = Regex("""<b>([^<]+)</b>""", RegexOption.IGNORE_CASE)
        var count = 0
        return bold.replace(text) { match ->
            if (count >= 12) return@replace match.value
            val name = match.groupValues[1]
            if (name.length < 2 || name.equals("Item:", true)) return@replace match.value
            val id = ItemDatabase.getByName(name)?.id ?: 0
            val links = if (id > 0) linkMarkup(name, id, preferences) else "[use][equip]"
            if (links.isEmpty()) return@replace match.value
            count++
            """${match.value} <font size=1 class="mafia-uselink">$links</font>"""
        }
    }

    private fun annotateAcquires(text: String, preferences: Preferences?): String {
        var out = text
        for (pattern in listOf(acquireItem, madeItem)) {
            out = pattern.replace(out) { match ->
                val name = match.groupValues[1]
                val id = ItemDatabase.getByName(name)?.id ?: 0
                val links = linkMarkup(name, id, preferences)
                if (links.isEmpty()) match.value
                else "${match.value} <font size=1 class=\"mafia-uselink\">$links</font>"
            }
        }
        return out
    }

    private fun annotateItemTables(text: String, preferences: Preferences?): String =
        itemTableRel.replace(text) { match ->
            val id = match.groupValues[1].toIntOrNull() ?: 0
            val name = ItemDatabase.getItemName(id)
            val links = linkMarkup(name, id, preferences)
            if (links.isEmpty() || match.value.contains("mafia-uselink")) match.value
            else match.value.replace(
                "</td></tr></table>",
                """ <font size=1 class="mafia-uselink">$links</font></td></tr></table>""",
            )
        }

    private fun linkMarkup(name: String, itemId: Int, preferences: Preferences?): String {
        if (name.isBlank() && itemId <= 0) return ""
        val id = if (itemId > 0) itemId else ItemDatabase.getByName(name)?.id ?: 0
        if (id <= 0) return ""
        val parts = mutableListOf<String>()
        val item = ItemDatabase.getById(id)
        if (ItemDatabase.isUsable(id)) {
            val useLabel = UseLinkSpeculation.getPotionSpeculation("use", id, preferences)
            parts += """[<a href="inv_use.php?whichitem=$id&ajax=1">$useLabel</a>]"""
        }
        if (item != null && item.primaryUse.isEquipment) {
            val equipLabel = UseLinkSpeculation.getEquipmentSpeculation("equip", id, preferences = preferences)
            parts += """[<a href="inv_equip.php?which=2&whichitem=$id&ajax=1">$equipLabel</a>]"""
        }
        if (ItemDatabase.isCookable(id) || ItemDatabase.isMixable(id) ||
            ItemDatabase.isSmithable(id) || ItemDatabase.isPasteable(id)
        ) {
            parts += """[<a href="craft.php?mode=cook&a=$id">create</a>]"""
        }
        return parts.joinToString(" ")
    }
}
