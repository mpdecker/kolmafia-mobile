package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.modifiers.BitmapModifier
import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.modifiers.CurrentModifiers
import net.sourceforge.kolmafia.modifiers.DerivedModifier
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierValues
import net.sourceforge.kolmafia.modifiers.StringModifier
import kotlin.math.abs

/**
 * Desktop [SpeculateCommand.getHTML] — modifier delta table for Relay UseLink tooltips
 * and CLI `whatif`/`speculate` prediction output.
 */
object SpeculateHtml {
    fun getHTML(
        speculated: CurrentModifiers,
        baseline: CurrentModifiers,
        attribs: String = "",
    ): String? = getHTML(speculated.values, speculated.derived, baseline.values, baseline.derived, attribs)

    fun getHTML(
        now: ModifierValues,
        nowDerived: Map<DerivedModifier, Int>,
        was: ModifierValues,
        wasDerived: Map<DerivedModifier, Int>,
        attribs: String = "",
    ): String? {
        val buf = StringBuilder("<table border=2 ")
        buf.append(attribs)
        buf.append(">")
        val startLen = buf.length
        for (mod in DoubleModifier.entries) {
            if (mod.subsumed.isNotEmpty()) continue
            val before = was.get(mod)
            val after = now.get(mod)
            if (after == before) continue
            appendNumeric(mod.tag, before, after, buf)
        }
        for (mod in DerivedModifier.entries) {
            val before = wasDerived[mod]?.toDouble() ?: 0.0
            val after = nowDerived[mod]?.toDouble() ?: 0.0
            if (after == before) continue
            appendNumeric(mod.displayName, before, after, buf)
        }
        for (mod in BitmapModifier.entries) {
            val before = was.get(mod).toDouble()
            val after = now.get(mod).toDouble()
            if (after == before) continue
            appendNumeric(mod.tag, before, after, buf)
        }
        for (mod in BooleanModifier.entries) {
            val before = was.get(mod)
            val after = now.get(mod)
            if (after == before) continue
            buf.append("<tr><td>").append(mod.tag).append("</td><td>")
                .append(after).append("</td></tr>")
        }
        for (mod in StringModifier.entries) {
            val before = was.get(mod).orEmpty()
            val after = now.get(mod).orEmpty()
            if (after == before) continue
            if (before.isEmpty()) {
                buf.append("<tr><td>").append(mod.tag).append("</td><td>")
                    .append(after.replace("\t", "<br>")).append("</td></tr>")
            } else {
                buf.append("<tr><td rowspan=2>").append(mod.tag).append("</td><td>")
                    .append(before.replace("\t", "<br>")).append("</td></tr><tr><td>")
                    .append(after.replace("\t", "<br>")).append("</td></tr>")
            }
        }
        if (buf.length > startLen) {
            buf.append("</table>")
            return buf.toString()
        }
        return null
    }

    private fun appendNumeric(name: String, was: Double, now: Double, buf: StringBuilder) {
        buf.append("<tr><td>").append(name).append("</td><td>")
            .append(formatFloat(now)).append(" (")
        if (now > was) buf.append("+")
        buf.append(formatFloat(now - was)).append(")</td></tr>")
    }

    private fun formatFloat(value: Double): String {
        if (abs(value - value.toLong()) < 1e-9) return value.toLong().toString()
        val rounded = (value * 100.0).toLong() / 100.0
        return if (abs(rounded - rounded.toLong()) < 1e-9) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }
}
