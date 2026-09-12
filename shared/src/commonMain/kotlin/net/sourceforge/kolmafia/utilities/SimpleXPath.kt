package net.sourceforge.kolmafia.utilities

import net.sourceforge.kolmafia.ash.ScriptException

/**
 * Minimal xpath evaluator for common KoL script patterns.
 * Mirrors desktop HtmlCleaner xpath for desc/adventure/account HTML fragments.
 *
 * Phase 6011–6025 (XXXVI): `contains(@attr,'lit')`, numeric position `[n]` /
 * `[last()]`, and mid-path attribute steps `//tag/@attr`.
 * Phase 6481–6490 (XLIII-F): child `/text()` and `following-sibling::` axes.
 */
object SimpleXPath {

    fun evaluate(html: String, xpath: String): List<String> {
        if (xpath.isBlank()) {
            return listOf(HtmlTreeParser.parse(html).serialize())
        }
        val (path, attributeSuffix) = splitAttributeSuffix(xpath)
        val root = HtmlTreeParser.parse(html)
        val nodes = try {
            evaluatePath(listOf(root), path)
        } catch (_: ScriptException) {
            throw ScriptException("invalid xpath expression")
        }
        return if (attributeSuffix != null) {
            nodes.map { it.attributes[attributeSuffix].orEmpty() }
        } else {
            nodes.map { node ->
                when {
                    node.isTextNode -> node.text
                    else -> node.serialize()
                }
            }
        }
    }

    private fun splitAttributeSuffix(xpath: String): Pair<String, String?> {
        if (xpath.startsWith("//@")) return xpath to null
        // Prefer "/@attr" mid-path form, then bare trailing "@attr".
        val slashMatch = Regex("""/@([A-Za-z_][\w-]*)$""").find(xpath)
        if (slashMatch != null) {
            val attr = slashMatch.groupValues[1]
            val path = xpath.removeSuffix("/@$attr")
            if (path.contains('@') && path.lastIndexOf('[') > path.lastIndexOf(']')) {
                return xpath to null
            }
            return path to attr.lowercase()
        }
        val match = Regex("""@([A-Za-z_][\w-]*)$""").find(xpath) ?: return xpath to null
        val attr = match.groupValues[1]
        val path = xpath.removeSuffix("@$attr")
        if (path.endsWith("//") || path.endsWith('/')) return xpath to null
        if (path.contains('@') && path.lastIndexOf('[') > path.lastIndexOf(']')) {
            return xpath to null
        }
        return path to attr.lowercase()
    }

    private fun evaluatePath(current: List<HtmlNode>, xpath: String): List<HtmlNode> {
        var remaining = xpath.trim()
        var nodes = current
        while (remaining.isNotEmpty()) {
            when {
                remaining.startsWith("//") -> {
                    val end = findStepEnd(remaining, 2)
                    val step = remaining.substring(2, end)
                    remaining = remaining.substring(end)
                    nodes = when {
                        step == "text()" -> return nodes.flatMap { collectTextNodes(it) }
                        step.startsWith("@") -> {
                            return nodes.flatMap {
                                collectAttributeValues(it, step.removePrefix("@").lowercase())
                            }
                        }
                        step.startsWith("following-sibling::") ->
                            evaluateFollowingSiblingStep(nodes, step.removePrefix("following-sibling::"))
                        else -> evaluateDescendantStep(nodes, step)
                    }
                }
                remaining.startsWith("/") -> {
                    val end = findStepEnd(remaining, 1)
                    val step = remaining.substring(1, end)
                    remaining = remaining.substring(end)
                    when {
                        step.startsWith("@") -> {
                            // Mid-path attribute step: take @attr from the current node set only
                            // (do not walk descendants — that would duplicate nested attrs).
                            val attr = step.removePrefix("@").lowercase()
                            return nodes.mapNotNull { node ->
                                if (node.isTextNode) null
                                else node.attributes[attr]?.let { textNode(it) }
                            }
                        }
                        step == "text()" -> {
                            nodes = nodes.flatMap { collectDirectTextNodes(it) }
                        }
                        step.startsWith("following-sibling::") -> {
                            nodes = evaluateFollowingSiblingStep(
                                nodes,
                                step.removePrefix("following-sibling::"),
                            )
                        }
                        else -> nodes = evaluateChildStep(nodes, step)
                    }
                }
                else -> throw ScriptException("invalid xpath expression")
            }
        }
        return nodes
    }

    private fun findStepEnd(xpath: String, start: Int): Int {
        var index = start
        var depth = 0
        while (index < xpath.length) {
            when (xpath[index]) {
                '[' -> depth++
                ']' -> depth--
                else -> {
                    if (depth == 0) {
                        if (index > start && xpath.startsWith("//", index)) return index
                        if (index > start && xpath[index] == '/') return index
                    }
                }
            }
            index++
        }
        return xpath.length
    }

    private fun evaluateDescendantStep(current: List<HtmlNode>, step: String): List<HtmlNode> {
        val parsed = parseStep(step)
        val matched = current.flatMap { node ->
            val descendants = mutableListOf<HtmlNode>()
            collectDescendants(node, descendants)
            descendants.filter { matches(it, parsed.tag, parsed.attrPredicates) }
        }
        return applyPosition(matched, parsed.position)
    }

    private fun evaluateChildStep(current: List<HtmlNode>, step: String): List<HtmlNode> {
        val parsed = parseStep(step)
        if (parsed.tag == null && step == "text()") {
            return current.flatMap { collectDirectTextNodes(it) }
        }
        val matched = current.flatMap { node ->
            node.children.filter { !it.isTextNode && matches(it, parsed.tag, parsed.attrPredicates) }
        }
        return applyPosition(matched, parsed.position)
    }

    private fun evaluateFollowingSiblingStep(current: List<HtmlNode>, step: String): List<HtmlNode> {
        val parsed = parseStep(step.ifBlank { "*" })
        val matched = current.flatMap { node ->
            followingSiblings(node).filter { sibling ->
                !sibling.isTextNode && matches(sibling, parsed.tag, parsed.attrPredicates)
            }
        }
        return applyPosition(matched, parsed.position)
    }

    private fun followingSiblings(node: HtmlNode): List<HtmlNode> {
        val parent = node.parent ?: return emptyList()
        val siblings = parent.children.filter { !it.isTextNode }
        val index = siblings.indexOf(node)
        if (index < 0) return emptyList()
        return siblings.drop(index + 1)
    }

    private fun applyPosition(nodes: List<HtmlNode>, position: PositionPredicate?): List<HtmlNode> {
        if (position == null) return nodes
        if (nodes.isEmpty()) return emptyList()
        return when (position) {
            is PositionPredicate.Index -> {
                val idx = position.oneBased - 1
                if (idx in nodes.indices) listOf(nodes[idx]) else emptyList()
            }
            PositionPredicate.Last -> listOf(nodes.last())
        }
    }

    private fun parseStep(step: String): ParsedStep {
        if (step == "text()") return ParsedStep(null, emptyList(), null)
        val tagMatch = Regex("""^([A-Za-z*][\w:-]*|\*)""").find(step)
            ?: throw ScriptException("invalid xpath expression")
        val tag = tagMatch.groupValues[1].lowercase()
        val predicatesRaw = step.substring(tagMatch.range.last + 1)
        if (predicatesRaw.isEmpty()) return ParsedStep(tag, emptyList(), null)

        val attrPredicates = mutableListOf<AttrPredicate>()
        var position: PositionPredicate? = null
        var index = 0
        while (index < predicatesRaw.length) {
            if (predicatesRaw[index] != '[') {
                throw ScriptException("invalid xpath expression")
            }
            val close = predicatesRaw.indexOf(']', index)
            if (close < 0) throw ScriptException("invalid xpath expression")
            val body = predicatesRaw.substring(index + 1, close).trim()
            when {
                body.equals("last()", ignoreCase = true) -> {
                    if (position != null) throw ScriptException("invalid xpath expression")
                    position = PositionPredicate.Last
                }
                body.toIntOrNull() != null -> {
                    if (position != null) throw ScriptException("invalid xpath expression")
                    val n = body.toInt()
                    if (n <= 0) throw ScriptException("invalid xpath expression")
                    position = PositionPredicate.Index(n)
                }
                else -> {
                    val contains = CONTAINS_PRED.matchEntire(body)
                    val exact = EXACT_ATTR_PRED.matchEntire(body)
                    when {
                        contains != null -> attrPredicates += AttrPredicate.Contains(
                            contains.groupValues[1].lowercase(),
                            contains.groupValues[3],
                        )
                        exact != null -> attrPredicates += AttrPredicate.Exact(
                            exact.groupValues[1].lowercase(),
                            exact.groupValues[3],
                        )
                        else -> throw ScriptException("invalid xpath expression")
                    }
                }
            }
            index = close + 1
        }
        return ParsedStep(tag, attrPredicates, position)
    }

    private fun matches(node: HtmlNode, tag: String?, predicates: List<AttrPredicate>): Boolean {
        if (node.isTextNode) return false
        if (tag != null && tag != "*" && node.tag != tag) return false
        return predicates.all { pred ->
            val value = node.attributes[pred.name].orEmpty()
            when (pred) {
                is AttrPredicate.Exact -> value == pred.value
                is AttrPredicate.Contains -> value.contains(pred.value)
            }
        }
    }

    private fun collectDescendants(node: HtmlNode, out: MutableList<HtmlNode>) {
        for (child in node.children) {
            if (!child.isTextNode) {
                out += child
                collectDescendants(child, out)
            }
        }
    }

    private fun collectAttributeValues(node: HtmlNode, attr: String): List<HtmlNode> {
        val out = mutableListOf<HtmlNode>()
        fun walk(current: HtmlNode) {
            if (!current.isTextNode) {
                current.attributes[attr]?.let { out += textNode(it) }
                current.children.forEach(::walk)
            }
        }
        walk(node)
        return out
    }

    private fun collectTextNodes(node: HtmlNode): List<HtmlNode> {
        val out = mutableListOf<HtmlNode>()
        fun walk(current: HtmlNode) {
            if (current.isTextNode) {
                if (current.text.isNotBlank()) out += current
            } else {
                if (current.text.isNotBlank()) out += textNode(current.text)
                current.children.forEach(::walk)
            }
        }
        walk(node)
        return out
    }

    /** Direct child text nodes only (xpath `/text()`). */
    private fun collectDirectTextNodes(node: HtmlNode): List<HtmlNode> {
        val out = mutableListOf<HtmlNode>()
        if (node.text.isNotBlank()) out += textNode(node.text)
        for (child in node.children) {
            if (child.isTextNode && child.text.isNotBlank()) out += child
        }
        return out
    }

    private fun textNode(text: String): HtmlNode = HtmlNode(tag = null, text = text)

    private sealed class AttrPredicate {
        abstract val name: String
        data class Exact(override val name: String, val value: String) : AttrPredicate()
        data class Contains(override val name: String, val value: String) : AttrPredicate()
    }

    private sealed class PositionPredicate {
        data class Index(val oneBased: Int) : PositionPredicate()
        data object Last : PositionPredicate()
    }

    private data class ParsedStep(
        val tag: String?,
        val attrPredicates: List<AttrPredicate>,
        val position: PositionPredicate?,
    )

    private val EXACT_ATTR_PRED =
        Regex("""^@([A-Za-z_][\w-]*)\s*=\s*(['"])(.*?)\2$""")
    private val CONTAINS_PRED =
        Regex("""^contains\(\s*@([A-Za-z_][\w-]*)\s*,\s*(['"])(.*?)\2\s*\)$""", RegexOption.IGNORE_CASE)
}
