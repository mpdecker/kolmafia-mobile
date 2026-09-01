package net.sourceforge.kolmafia.utilities

/**
 * Minimal HTML tree for xpath evaluation — not a full DOM implementation.
 */
internal data class HtmlNode(
    val tag: String?,
    val attributes: Map<String, String> = emptyMap(),
    val children: MutableList<HtmlNode> = mutableListOf(),
    var text: String = "",
) {
    val isTextNode: Boolean get() = tag == null

    fun serialize(): String = when {
        isTextNode -> text
        tag == "#document" -> children.joinToString("") { it.serialize() }
        else -> buildString {
            append('<').append(tag)
            for ((name, value) in attributes) {
                append(' ').append(name).append('=').append('"')
                append(escapeAttribute(value)).append('"')
            }
            if (children.isEmpty() && text.isBlank()) {
                append("/>")
            } else {
                append('>')
                if (text.isNotBlank()) append(text)
                for (child in children) append(child.serialize())
                append("</").append(tag).append('>')
            }
        }
    }

    private fun escapeAttribute(value: String): String =
        value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;")
}

internal object HtmlTreeParser {
    private val VOID_TAGS = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input",
        "link", "meta", "param", "source", "track", "wbr",
    )

    fun parse(html: String): HtmlNode {
        val root = HtmlNode("#document")
        parseInto(root, html)
        return root
    }

    private fun parseInto(parent: HtmlNode, html: String) {
        var index = 0
        while (index < html.length) {
            val lt = html.indexOf('<', index)
            if (lt == -1) {
                appendText(parent, html.substring(index))
                break
            }
            if (lt > index) appendText(parent, html.substring(index, lt))
            if (html.startsWith("<!--", lt)) {
                val end = html.indexOf("-->", lt + 4)
                index = if (end == -1) html.length else end + 3
                continue
            }
            if (html.startsWith("<!", lt) || html.startsWith("<?", lt)) {
                val end = html.indexOf('>', lt)
                index = if (end == -1) html.length else end + 1
                continue
            }
            if (html.startsWith("</", lt)) {
                return
            }
            val gt = findTagEnd(html, lt)
            if (gt == -1) {
                appendText(parent, html.substring(lt))
                break
            }
            val tagText = html.substring(lt + 1, gt).trim()
            if (tagText.endsWith('/')) {
                val selfClosing = tagText.removeSuffix("/").trim()
                val (tag, attrs) = parseTag(selfClosing)
                if (tag != null) parent.children += HtmlNode(tag, attrs)
                index = gt + 1
                continue
            }
            val (tag, attrs) = parseTag(tagText)
            if (tag == null) {
                index = gt + 1
                continue
            }
            val node = HtmlNode(tag, attrs)
            parent.children += node
            index = gt + 1
            if (tag.lowercase() in VOID_TAGS) continue
            parseInto(node, html.substring(index))
            val close = "</$tag>"
            val closeIndex = html.indexOf(close, index, ignoreCase = true)
            index = if (closeIndex == -1) html.length else closeIndex + close.length
        }
    }

    private fun appendText(parent: HtmlNode, raw: String) {
        if (raw.isEmpty()) return
        val decoded = raw.replace("&nbsp;", " ")
        if (parent.children.lastOrNull()?.isTextNode == true) {
            parent.children.last().text += decoded
        } else {
            parent.children += HtmlNode(tag = null, text = decoded)
        }
    }

    private fun findTagEnd(html: String, start: Int): Int {
        var quote: Char? = null
        for (i in start + 1 until html.length) {
            when (val c = html[i]) {
                '\'', '"' -> {
                    if (quote == null) quote = c
                    else if (quote == c) quote = null
                }
                '>' -> if (quote == null) return i
            }
        }
        return -1
    }

    private fun parseTag(raw: String): Pair<String?, Map<String, String>> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null to emptyMap()
        val parts = Regex("""(\S+)(.*)""").find(trimmed) ?: return null to emptyMap()
        val tag = parts.groupValues[1].lowercase()
        val attrText = parts.groupValues[2]
        val attrs = linkedMapOf<String, String>()
        Regex("""([A-Za-z_:][\w:.-]*)\s*=\s*(['"])(.*?)\2""").findAll(attrText).forEach { match ->
            attrs[match.groupValues[1].lowercase()] = match.groupValues[3]
        }
        return tag to attrs
    }
}
