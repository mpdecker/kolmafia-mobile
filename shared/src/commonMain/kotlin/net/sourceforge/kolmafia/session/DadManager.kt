package net.sourceforge.kolmafia.session

/** Headless Dad Sea Monkee clue solver. */
object DadManager {
    enum class Element { NONE, HOT, COLD, STENCH, SPOOKY, SLEAZE, PHYSICAL }

    private val cluePattern = Regex(
        """You shake your head and look above the tank, at the window into space\. *([^ ]+) forms ([^ ]+) in the darkness, each more ([^ ]+) than the last\. *(?:The )?([^ ]+) ([^,]+), ([^ ]+) revealing (\d+)-dimensional monstrosities\..*?No\. *Look again\. *There is nothing\. *(?:Is|Are) your (.+?) betraying you\? *As if on cue, (\d+)-sided triangles materialize and then disappear\. *So impossible that your ([^ ]+) throbs\.""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    val elementalWeakness: Array<Element> = Array(11) { Element.NONE }

    private val wordTables = listOf(
        arrayOf("chaotic", "rigid", "rotting", "horrifying", "slimy", "pulpy"),
        arrayOf("skitter", "shamble", "ooze", "float", "slither", "swim"),
        arrayOf("terrible", "awful", "putrescent", "frightening", "bloated", "curious"),
        arrayOf("blackness", "space", "void", "darkness", "emptiness", "portal"),
        arrayOf("warps", "shifts", "shimmers", "shakes", "wobbles", "cracks open"),
    )
    private val elements = arrayOf(
        Element.HOT, Element.COLD, Element.STENCH,
        Element.SPOOKY, Element.SLEAZE, Element.PHYSICAL,
    )
    private val word8 = arrayOf(
        "brain", "mind", "reason", "sanity", "grasp on reality",
        "sixth sense", "eyes", "thoughts", "senses", "memories", "fears",
    )
    private val word10 = arrayOf(
        "spleen", "stomach", "skull", "forehead", "brain",
        "mind", "heart", "throat", "chest", "head",
    )

    fun weakness(round: Int): Element =
        if (round in 1..10) elementalWeakness[round] else Element.NONE

    fun intToElement(index: Int): Element =
        Element.entries.getOrNull(index) ?: Element.NONE

    fun elementToName(element: Element): String = when (element) {
        Element.NONE -> "none"
        Element.HOT -> "hot"
        Element.COLD -> "cold"
        Element.STENCH -> "stench"
        Element.SPOOKY -> "spooky"
        Element.SLEAZE -> "sleaze"
        Element.PHYSICAL -> "physical"
    }

    fun elementToSpell(element: Element, hasSkill: (String) -> Boolean = { false }): String = when (element) {
        Element.HOT -> listOf("Awesome Balls of Fire", "Volcanometeor Showeruption")
        Element.COLD -> listOf("Snowclone")
        Element.STENCH -> listOf("Eggsplosion")
        Element.SPOOKY -> listOf("Raise Backup Dancer")
        Element.SLEAZE -> listOf("Grease Lightning")
        Element.PHYSICAL -> listOf("Toynado", "Shrap")
        Element.NONE -> emptyList()
    }.firstOrNull(hasSkill) ?: "Unknown"

    fun solve(responseText: String?): Boolean {
        val match = responseText?.let { cluePattern.find(it) } ?: return false
        val first = wordElement(match.groupValues[1], 0) ?: return false
        val second = wordElement(match.groupValues[2], 1) ?: return false
        val third = wordElement(match.groupValues[3], 2) ?: return false
        val fourth = wordElement(match.groupValues[4], 3) ?: return false
        val fifth = wordElement(match.groupValues[5], 4) ?: return false
        val order = match.groupValues[6].trim().lowercase()
        if (order != "slowly" && order != "suddenly") return false
        val number = match.groupValues[7].toIntOrNull() ?: return false
        if (number !in 0..63) return false
        val eighth = word8.indexOfFirst { it.equals(match.groupValues[8], true) }
        if (eighth < 0) return false
        val ninth = match.groupValues[9].toIntOrNull() ?: return false
        val tenth = word10.indexOfFirst { it.equals(match.groupValues[10], true) }
        if (tenth < 0) return false
        val solved = Array(11) { Element.NONE }
        solved[1] = first
        solved[2] = second
        solved[3] = third
        solved[4] = fifth
        solved[5] = fourth
        val bits = elements.indices.reversed()
            .filter { number and (1 shl it) != 0 }
            .map { elements[it] }
        val pair = when {
            bits.isEmpty() -> listOf(Element.PHYSICAL, Element.PHYSICAL)
            bits.size == 1 -> listOf(bits[0], bits[0])
            else -> bits.take(2)
        }
        val reverse = order == "suddenly"
        solved[6] = if (reverse) pair[1] else pair[0]
        solved[7] = if (reverse) pair[0] else pair[1]

        val valueEight = eighth + 2 - solved[1].ordinal
        solved[8] = intToElement(valueEight)
        if (solved[8] == Element.NONE) return false

        val nine = match.groupValues[9].toIntOrNull() ?: return false
        val valueNine = solved.slice(2..5).sumOf { it.ordinal } + 4 - nine
        solved[9] = intToElement(valueNine)
        if (solved[9] == Element.NONE) return false

        val ten = word10.indexOfFirst { it.equals(match.groupValues[10], true) } + 1
        solved[10] = when {
            ten in 1..9 -> solved[ten]
            ten == 10 -> unusedElement(solved)
            else -> return false
        }
        elementalWeakness.indices.forEach { elementalWeakness[it] = solved[it] }
        return true
    }

    private fun wordElement(word: String, table: Int): Element? =
        elements.getOrNull(wordTables[table].indexOfFirst { it.equals(word, true) })

    private fun unusedElement(values: Array<Element>): Element =
        elements.firstOrNull { it !in values.slice(1..9) } ?: Element.NONE
}
