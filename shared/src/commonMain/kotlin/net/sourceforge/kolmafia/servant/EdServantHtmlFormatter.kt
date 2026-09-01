package net.sourceforge.kolmafia.servant

import net.sourceforge.kolmafia.modifiers.ServantData

/** Desktop EdServantCommand.printServants HTML table (headless; no image download). */
object EdServantHtmlFormatter {

    fun buildServantsTable(manager: EdServantManager): String {
        val output = StringBuilder()
        output.append("<table border=2 cols=4 cellpadding=5>")
        output.append("<tr>")
        output.append("<th>Type</th><th>Image</th><th>Name</th><th>Abilities</th>")
        output.append("</tr>")

        for (data in allServants()) {
            val type = data.type
            val image = "itemimages/${data.image}"
            val record = manager.findEdServant(type)

            output.append("<tr>")
            output.append("<td>").append(escape(type)).append("</td>")
            output.append("<td><img src=\"/").append(escape(image))
                .append("\" alt=\"").append(escape(type)).append("\"></td>")
            output.append("<td>")
            if (record == null) {
                output.append("-")
            } else {
                output.append("<table border=0 cols=1 cellpadding=0>")
                output.append("<tr><td></td></tr>")
                output.append("<tr><td>").append(escape(record.name)).append("</td></tr>")
                output.append("<tr><td>(Level ").append(record.level)
                    .append(", ").append(record.experience).append(" XP)</td></tr>")
                output.append("<tr><td></td></tr>")
                output.append("</table>")
            }
            output.append("</td>")
            output.append("<td>")
            output.append("<table border=0 cols=1 cellpadding=0>")
            appendAbilityRow(output, 1, data.level1Ability)
            appendAbilityRow(output, 7, data.level7Ability)
            appendAbilityRow(output, 14, data.level14Ability)
            appendAbilityRow(output, 21, data.level21Ability)
            output.append("</table>")
            output.append("</td>")
            output.append("</tr>")
        }

        output.append("</table>")
        return output.toString()
    }

    fun buildCurrentServantLine(manager: EdServantManager): String {
        val active = manager.activeServantRecord()
        return if (active == null) {
            "You do not currently have an active servant"
        } else {
            "Your current servant is ${active.name}, the ${active.type} " +
                "(level ${active.level}, ${active.experience} xp)"
        }
    }

    fun buildSummonedStatusLines(manager: EdServantManager): List<String> {
        val summoned = manager.getSummonedTypes()
        if (summoned.isEmpty()) return listOf("No entombed servants summoned.")
        return buildList {
            for (type in summoned) {
                val record = manager.findEdServant(type)
                add(
                    record?.let { "${it.name}, the ${it.type} (level ${it.level}, ${it.experience} xp)" }
                        ?: type,
                )
            }
            manager.activeServantType().takeIf { it.isNotBlank() }?.let { add("Active servant: $it") }
        }
    }

    private fun allServants(): List<ServantData.Servant> =
        (1..7).mapNotNull { ServantData.servantForId(it) }

    private fun appendAbilityRow(output: StringBuilder, level: Int, ability: String) {
        output.append("<tr style=\"text-align:left\"><td>Level $level: ")
            .append(escape(ability)).append("</td></tr>")
    }

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
