package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.Beeosity
import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.modifiers.DoubleModifier

/** Parsed maximize goal: weighted stat expression plus optional constraints. */
data class MaximizeSpec(
    val primary: DoubleModifier,
    val evaluator: Evaluator,
    val requiredBooleans: Set<BooleanModifier> = emptySet(),
    val forbiddenBooleans: Set<BooleanModifier> = emptySet(),
    val equipRequired: List<String> = emptyList(),
    val switchFamiliars: List<String> = emptyList(),
    val switchThralls: List<String> = emptyList(),
    val enthronedFamiliars: List<String> = emptyList(),
    val bjornifiedFamiliars: List<String> = emptyList(),
    val requireMelee: Boolean = false,
    val requireHands: Boolean = false,
    val maxPrice: Int? = null,
    val minPrice: Int? = null,
    val allowCreatable: Boolean = false,
    val forbidCreatable: Boolean = false,
    /** Desktop Evaluator beeosity limit (default 2). */
    val maxBeeosity: Int = 2,
) {
    /** Test helper — builds a single-stat evaluator from [primary]. */
    constructor(
        primary: DoubleModifier,
        requiredBooleans: Set<BooleanModifier> = emptySet(),
        forbiddenBooleans: Set<BooleanModifier> = emptySet(),
        equipRequired: List<String> = emptyList(),
        switchFamiliars: List<String> = emptyList(),
        switchThralls: List<String> = emptyList(),
        enthronedFamiliars: List<String> = emptyList(),
        bjornifiedFamiliars: List<String> = emptyList(),
        requireMelee: Boolean = false,
        requireHands: Boolean = false,
        maxPrice: Int? = null,
        minPrice: Int? = null,
        allowCreatable: Boolean = false,
        forbidCreatable: Boolean = false,
        maxBeeosity: Int = 2,
    ) : this(
        primary = primary,
        evaluator = Evaluator(statKeyword(primary)),
        requiredBooleans = requiredBooleans,
        forbiddenBooleans = forbiddenBooleans,
        equipRequired = equipRequired,
        switchFamiliars = switchFamiliars,
        switchThralls = switchThralls,
        enthronedFamiliars = enthronedFamiliars,
        bjornifiedFamiliars = bjornifiedFamiliars,
        requireMelee = requireMelee,
        requireHands = requireHands,
        maxPrice = maxPrice,
        minPrice = minPrice,
        allowCreatable = allowCreatable,
        forbidCreatable = forbidCreatable,
        maxBeeosity = maxBeeosity,
    )

    companion object {
        internal fun statKeyword(primary: DoubleModifier): String = when (primary) {
            DoubleModifier.MUS -> "muscle"
            DoubleModifier.MYS -> "mysticality"
            DoubleModifier.MOX -> "moxie"
            DoubleModifier.HP -> "hp"
            DoubleModifier.MP -> "mp"
            DoubleModifier.INITIATIVE -> "initiative"
            DoubleModifier.MEATDROP -> "meat"
            DoubleModifier.ITEMDROP -> "item"
            DoubleModifier.EXPERIENCE -> "exp"
            else -> primary.tag.lowercase()
        }
    }
}

/** Parses desktop-style maximize goal strings into modifier tags and constraints. */
object MaximizeGoal {

    fun parseSpec(goal: String): MaximizeSpec? {
        val trimmed = goal.trim()
        if (trimmed.isBlank()) return null

        val statTerms = mutableListOf<String>()
        val required = mutableSetOf<BooleanModifier>()
        val forbidden = mutableSetOf<BooleanModifier>()
        val equip = mutableListOf<String>()
        val switches = mutableListOf<String>()
        val switchThralls = mutableListOf<String>()
        val enthrones = mutableListOf<String>()
        val bjorns = mutableListOf<String>()
        var requireMelee = false
        var requireHands = false
        var maxPrice: Int? = null
        var minPrice: Int? = null
        var allowCreatable = false
        var forbidCreatable = false
        var maxBeeosity = 2

        val terms = if (trimmed.contains(',')) splitTerms(trimmed) else listOf(trimmed)
        for (term in terms) {
            val t = term.trim()
            if (t.isBlank()) continue
            if (!applyConstraintTerm(
                    t, required, forbidden, equip, switches, switchThralls,
                    enthrones, bjorns,
                    { requireMelee = true },
                    { requireHands = true },
                    { maxPrice = it },
                    { minPrice = it },
                    { allowCreatable = true },
                    { forbidCreatable = true },
                    { maxBeeosity = it },
                )
            ) {
                statTerms.add(t)
            }
        }

        if (statTerms.isEmpty()) return null

        val evaluator = Evaluator(statTerms.joinToString(", "))
        evaluator.applyBooleanConstraints(required, forbidden)
        val primary = evaluator.highestWeightedStat() ?: DoubleModifier.MUS
        return applyEquipBeeosityFloor(
            MaximizeSpec(
                primary = primary,
                evaluator = evaluator,
                requiredBooleans = required,
                forbiddenBooleans = forbidden,
                equipRequired = equip,
                switchFamiliars = switches,
                switchThralls = switchThralls,
                enthronedFamiliars = enthrones,
                bjornifiedFamiliars = bjorns,
                requireMelee = requireMelee,
                requireHands = requireHands,
                maxPrice = maxPrice,
                minPrice = minPrice,
                allowCreatable = allowCreatable,
                forbidCreatable = forbidCreatable,
                maxBeeosity = maxBeeosity,
            ),
        )
    }

    internal fun applyEquipBeeosityFloor(spec: MaximizeSpec): MaximizeSpec {
        val equipFloor = spec.equipRequired.sumOf { Beeosity.itemBeeosity(it) }
        return if (equipFloor > spec.maxBeeosity) {
            spec.copy(maxBeeosity = equipFloor)
        } else {
            spec
        }
    }

    fun parse(goal: String): DoubleModifier? = parseSpec(goal)?.primary

    internal fun parseSingleModifier(text: String): DoubleModifier? {
        val normalized = text.trim().lowercase()
        return when (normalized) {
            "all" -> DoubleModifier.MUS
            "mus", "muscle", "muscularity" -> DoubleModifier.MUS
            "mys", "myst", "mysticality" -> DoubleModifier.MYS
            "mox", "moxie" -> DoubleModifier.MOX
            "hp", "hit points", "maxhp" -> DoubleModifier.HP
            "mp", "mana", "maxmp" -> DoubleModifier.MP
            "init", "initiative" -> DoubleModifier.INITIATIVE
            "meat", "meat drop" -> DoubleModifier.MEATDROP
            "item", "item drop" -> DoubleModifier.ITEMDROP
            "exp", "experience" -> DoubleModifier.EXPERIENCE
            "abs", "absorb" -> DoubleModifier.ABSORB_STAT
            else -> DoubleModifier.byTag(text.trim())
        }
    }

    private fun applyConstraintTerm(
        t: String,
        required: MutableSet<BooleanModifier>,
        forbidden: MutableSet<BooleanModifier>,
        equip: MutableList<String>,
        switches: MutableList<String>,
        switchThralls: MutableList<String>,
        enthrones: MutableList<String>,
        bjorns: MutableList<String>,
        setRequireMelee: () -> Unit,
        setRequireHands: () -> Unit,
        setMaxPrice: (Int?) -> Unit,
        setMinPrice: (Int?) -> Unit,
        setAllowCreatable: () -> Unit,
        setForbidCreatable: () -> Unit,
        setMaxBeeosity: (Int) -> Unit,
    ): Boolean = when {
        t.equals("beeosity", ignoreCase = true) -> {
            setMaxBeeosity(1)
            true
        }
        t.startsWith("beeosity ", ignoreCase = true) -> {
            setMaxBeeosity(t.substring(9).trim().toIntOrNull() ?: 2)
            true
        }
        t.startsWith("equip ", ignoreCase = true) -> {
            equip.add(unquote(t.substring(6).trim()))
            true
        }
        t.startsWith("switch thrall ", ignoreCase = true) -> {
            switchThralls.add(unquote(t.substring(14).trim()))
            true
        }
        t.startsWith("switch ", ignoreCase = true) -> {
            switches.add(unquote(t.substring(7).trim()))
            true
        }
        t.startsWith("enthrone ", ignoreCase = true) -> {
            enthrones.add(unquote(t.substring(9).trim()))
            true
        }
        t.startsWith("bjornify ", ignoreCase = true) -> {
            bjorns.add(unquote(t.substring(9).trim()))
            true
        }
        t.equals("+melee", ignoreCase = true) -> {
            setRequireMelee()
            true
        }
        t.equals("+hands", ignoreCase = true) -> {
            setRequireHands()
            true
        }
        t.equals("creatable", ignoreCase = true) -> {
            setAllowCreatable()
            true
        }
        t.equals("-nocreat", ignoreCase = true) -> {
            setForbidCreatable()
            true
        }
        t.equals("-price", ignoreCase = true) -> {
            setMaxPrice(0)
            true
        }
        t.startsWith("-price ", ignoreCase = true) -> {
            setMaxPrice(t.substring(7).trim().toIntOrNull() ?: 0)
            true
        }
        t.startsWith("+price ", ignoreCase = true) -> {
            setMinPrice(t.substring(7).trim().toIntOrNull() ?: 0)
            true
        }
        t.startsWith('-') -> {
            val tag = unquote(t.drop(1).trim())
            BooleanModifier.byTag(tag)?.let { forbidden.add(it) } != null
        }
        t.startsWith('+') -> {
            val rest = unquote(t.drop(1).trim())
            when {
                BooleanModifier.byTag(rest) != null -> {
                    required.add(BooleanModifier.byTag(rest)!!)
                    true
                }
                else -> false
            }
        }
        else -> BooleanModifier.byTag(t)?.let { required.add(it) } != null
    }

    private fun splitTerms(goal: String): List<String> {
        val terms = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in goal) {
            when {
                ch == '"' -> {
                    inQuotes = !inQuotes
                    current.append(ch)
                }
                ch == ',' && !inQuotes -> {
                    terms.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotBlank()) terms.add(current.toString())
        return terms
    }

    private fun unquote(text: String): String {
        val t = text.trim()
        if (t.length >= 2 && t.first() == '"' && t.last() == '"') {
            return t.substring(1, t.length - 1).trim()
        }
        return t
    }
}
