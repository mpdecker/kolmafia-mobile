package net.sourceforge.kolmafia.utilities

import net.sourceforge.kolmafia.ash.ScriptException

/**
 * Minimal xpath evaluator for common KoL script patterns.
 * Mirrors desktop HtmlCleaner xpath for desc/adventure/account HTML fragments.
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
                    nodes = when (step) {
                        "text()" -> return nodes.flatMap { collectTextNodes(it) }
                        else -> {
                            if (step.startsWith("@")) {
                                return nodes.flatMap {
                                    collectAttributeValues(it, step.removePrefix("@").lowercase())
                                }
                            }
                            evaluateDescendantStep(nodes, step)
                        }
                    }
                }
                remaining.startsWith("/") -> {
                    val end = findStepEnd(remaining, 1)
                    val step = remaining.substring(1, end)
                    remaining = remaining.substring(end)
                    nodes = evaluateChildStep(nodes, step)
                }
                else -> throw ScriptException("invalid xpath expression")
            }
        }
        return nodes
    }

    private fun findStepEnd(xpath: String, start: Int): Int {
        var index = start
        while (index < xpath.length) {
            if (index > start && xpath.startsWith("//", index)) return index
            if (index > start && xpath[index] == '/') return index
            index++
        }
        return xpath.length
    }

    private fun evaluateDescendantStep(current: List<HtmlNode>, step: String): List<HtmlNode> {
        val (tag, predicates) = parseStep(step)
        return current.flatMap { node ->
            val descendants = mutableListOf<HtmlNode>()
            collectDescendants(node, descendants)
            descendants.filter { matches(it, tag, predicates) }
        }
    }

    private fun evaluateChildStep(current: List<HtmlNode>, step: String): List<HtmlNode> {
        val (tag, predicates) = parseStep(step)
        return current.flatMap { node ->
            node.children.filter { !it.isTextNode && matches(it, tag, predicates) }
        }
    }

    private fun parseStep(step: String): Pair<String?, List<AttrPredicate>> {
        if (step == "text()") return null to emptyList()
        val tagMatch = Regex("""^([A-Za-z*][\w:-]*|\*)""").find(step)
            ?: throw ScriptException("invalid xpath expression")
        val tag = tagMatch.groupValues[1].lowercase()
        val predicates = mutableListOf<AttrPredicate>()
        Regex("""\[@([A-Za-z_][\w-]*)\s*=\s*(['"])(.*?)\2\]""").findAll(step).forEach { match ->
            predicates += AttrPredicate(match.groupValues[1].lowercase(), match.groupValues[3])
        }
        if (step.contains('[') && predicates.isEmpty()) {
            throw ScriptException("invalid xpath expression")
        }
        return tag to predicates
    }

    private fun matches(node: HtmlNode, tag: String?, predicates: List<AttrPredicate>): Boolean {
        if (node.isTextNode) return false
        if (tag != null && tag != "*" && node.tag != tag) return false
        return predicates.all { node.attributes[it.name] == it.value }
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

    private fun textNode(text: String): HtmlNode = HtmlNode(tag = null, text = text)

    private data class AttrPredicate(val name: String, val value: String)
}
