package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.DoubleModifier.COLD_DAMAGE
import net.sourceforge.kolmafia.modifiers.DoubleModifier.COLD_RESISTANCE
import net.sourceforge.kolmafia.modifiers.DoubleModifier.COLD_SPELL_DAMAGE
import net.sourceforge.kolmafia.modifiers.DoubleModifier.DAMAGE_ABSORPTION
import net.sourceforge.kolmafia.modifiers.DoubleModifier.DAMAGE_REDUCTION
import net.sourceforge.kolmafia.modifiers.DoubleModifier.FAMILIAR_DAMAGE
import net.sourceforge.kolmafia.modifiers.DoubleModifier.FAMILIAR_EXP
import net.sourceforge.kolmafia.modifiers.DoubleModifier.FAMILIAR_WEIGHT
import net.sourceforge.kolmafia.modifiers.DoubleModifier.HOT_DAMAGE
import net.sourceforge.kolmafia.modifiers.DoubleModifier.HOT_RESISTANCE
import net.sourceforge.kolmafia.modifiers.DoubleModifier.HOT_SPELL_DAMAGE
import net.sourceforge.kolmafia.modifiers.DoubleModifier.HP
import net.sourceforge.kolmafia.modifiers.DoubleModifier.HP_REGEN_MAX
import net.sourceforge.kolmafia.modifiers.DoubleModifier.HP_REGEN_MIN
import net.sourceforge.kolmafia.modifiers.DoubleModifier.ITEMDROP
import net.sourceforge.kolmafia.modifiers.DoubleModifier.MEATDROP
import net.sourceforge.kolmafia.modifiers.DoubleModifier.MONSTER_LEVEL
import net.sourceforge.kolmafia.modifiers.DoubleModifier.MOX
import net.sourceforge.kolmafia.modifiers.DoubleModifier.MP
import net.sourceforge.kolmafia.modifiers.DoubleModifier.MP_REGEN_MAX
import net.sourceforge.kolmafia.modifiers.DoubleModifier.MP_REGEN_MIN
import net.sourceforge.kolmafia.modifiers.DoubleModifier.MUS
import net.sourceforge.kolmafia.modifiers.DoubleModifier.MYS
import net.sourceforge.kolmafia.modifiers.DoubleModifier.SLEAZE_DAMAGE
import net.sourceforge.kolmafia.modifiers.DoubleModifier.SLEAZE_RESISTANCE
import net.sourceforge.kolmafia.modifiers.DoubleModifier.SLEAZE_SPELL_DAMAGE
import net.sourceforge.kolmafia.modifiers.DoubleModifier.SPOOKY_DAMAGE
import net.sourceforge.kolmafia.modifiers.DoubleModifier.SPOOKY_RESISTANCE
import net.sourceforge.kolmafia.modifiers.DoubleModifier.SPOOKY_SPELL_DAMAGE
import net.sourceforge.kolmafia.modifiers.DoubleModifier.STENCH_DAMAGE
import net.sourceforge.kolmafia.modifiers.DoubleModifier.STENCH_RESISTANCE
import net.sourceforge.kolmafia.modifiers.DoubleModifier.STENCH_SPELL_DAMAGE
import net.sourceforge.kolmafia.utilities.PHPMTRandom
import net.sourceforge.kolmafia.utilities.PHPRandom

/** Desktop [WardrobeOMaticDatabase] — futuristic shirt/hat/collar RNG. */
object WardrobeOMaticDatabase {
    data class FuturisticClothing(
        val name: String,
        val image: String,
        val modifiers: Map<DoubleModifier, Int>,
    )

    private val shirtAdjectives = listOf(
        "galvanized",
        "double-creased",
        "double-breasted",
        "foil-clad",
        "aluminum-threaded",
        "electroplated",
        "carbon-coated",
        "phase-changing",
        "liquid cooled",
        "conductive",
        "radation-shielded",
        "nanotube-threaded",
        "moisture-wicking",
        "shape-memory",
        "antimicrobial",
        "liquid-cooled",
    )

    private val shirtMaterials = listOf(
        "gabardine",
        "mylar",
        "polyester",
        "double-polyester",
        "triple-polyester",
        "rayon",
        "wax paper",
        "aluminum foil",
        "synthetic silk",
        "xylon",
        "gore-tex",
        "kapton",
        "flannel",
        "silk",
        "cotton",
        "linen",
        "wool",
    )

    private val shirtQualities = listOf(
        "super",
        "hyper",
        "ultra",
        "mega",
        "reroll",
    )

    private val shirts = listOf(
        "t-shirt",
        "sweater",
        "jersey",
        "polo shirt",
        "dress shirt",
        "Neo-Hawaiian shirt",
        "sweatshirt",
    )

    private val hatAdjectives = listOf(
        "nanoplated",
        "self-replicating",
        "autonomous",
        "fusion-powered",
        "fision-powered",
        "hyperefficient",
        "quantum",
        "nuclear",
        "magnetic",
        "laser-guided",
        "solar-powered",
        "psionic",
        "gravitronic",
        "biotronic",
        "neurolinked",
        "transforming",
        "meta-fashionable",
    )

    private val hatMaterials = listOf(
        "tungsten",
        "carbon",
        "steel",
        "aluminum",
        "titanium",
        "iron",
        "hafnium",
        "nickel",
        "zinc",
        "lead",
        "platinum",
        "copper",
        "silver",
        "tantalum",
        "niobium",
        "palladium",
        "iridium",
        "bismuth",
        "cobalt",
        "indium",
        "molybdenum",
        "vanadium",
        "yttrium",
        "antimony",
    )

    private val hatEmphasis = listOf(
        "super",
        "ultra",
        "mega",
        "double-",
        "gamma-",
        "uber-",
        "great-",
        "grand-",
        "maxi-",
        "multi-",
        "tri-",
        "duo-",
        "gargantu-",
        "crypto-",
        "hyper",
        "cyber-",
        "astro-",
        "grav-",
    )

    private val hats = listOf(
        "beanie",
        "fedora",
        "trilby",
        "beret",
        "visor",
        "turban",
        "fez",
        "balaclava",
        "tam",
        "sombrero",
        "bowler",
        "cloche",
        "tiara",
        "snood",
        "diadem",
        "crown",
        "bandana",
        "cowl",
        "capuchon",
    )

    private val collars = listOf(
        "pet tag",
        "collar",
        "pet sweater",
    )

    private val collarAdjectives = listOf(
        "hyperchromatic",
        "pearlescent",
        "bright",
        "day-glo",
        "luminescent",
        "vibrant",
        "earthy",
        "oversaturated",
        "partially transparent",
        "opaque",
        "faded",
        "metallic",
        "shiny",
        "glow-in-the-dark",
        "neon",
        "prismatic",
        "incandescent",
        "polychromatic",
        "opalescent",
        "psychedelic",
        "kaleidoscopic",
    )

    private val collarColors = listOf(
        "amber",
        "aquamarine",
        "auburn",
        "azure",
        "beige",
        "black",
        "blue",
        "brown",
        "burgundy",
        "cerulean",
        "chartreuse",
        "cornflower",
        "cream",
        "crimson",
        "cyan",
        "ecru",
        "emerald",
        "fuchsia",
        "golden",
        "gray",
        "green",
        "indigo",
        "lavender",
        "lilac",
        "magenta",
        "maroon",
        "mauve",
        "mustard",
        "navy",
        "ochre",
        "olive",
        "orange",
        "periwinkle",
        "pink",
        "puce",
        "purple",
        "red",
        "rose",
        "ruby",
        "salmon",
        "scarlet",
        "sepia",
        "sienna",
        "silver",
        "tan",
        "taupe",
        "teal",
        "turquoise",
        "ultramarine",
        "vermilion",
        "violet",
        "viridian",
        "white",
        "yellow",
    )


    fun shirt(day: Int, tier: Int): FuturisticClothing {
        val seed = day * 11391L + 2063L
        val mtRand = PHPMTRandom(seed)
        val rand = PHPRandom(seed)
        val imageNum = mtRand.nextInt(1, 9)

        mtRand.nextDouble()
        val adjective = mtRand.pickOne(shirtAdjectives)
        val matRoll = mtRand.nextInt(0, shirtMaterials.size - 1)
        val material = shirtMaterials[matRoll]
        var quality = ""
        if (matRoll > 11) {
            quality = "reroll"
            while (quality == "reroll") {
                quality = mtRand.pickOne(shirtQualities)
            }
        }
        val shirtName = mtRand.pickOne(shirts)

        val shirtModifiers = mutableListOf(
            MUS, MYS, MOX,
            HOT_RESISTANCE, COLD_RESISTANCE, STENCH_RESISTANCE, SLEAZE_RESISTANCE, SPOOKY_RESISTANCE,
            HP, MP, HP_REGEN_MIN, MP_REGEN_MIN, DAMAGE_REDUCTION, DAMAGE_ABSORPTION,
        )
        if (tier > 3) {
            shirtModifiers.addAll(listOf(ITEMDROP, MEATDROP, MONSTER_LEVEL))
        }
        rand.shuffle(shirtModifiers)
        val modMap = linkedMapOf<DoubleModifier, Int>()
        for (i in 0 until tier) {
            val mod = shirtModifiers[i]
            modMap[mod] = modStrength(mtRand, mod, tier - 1)
            when (mod) {
                HP_REGEN_MIN -> modMap[HP_REGEN_MAX] = modStrength(mtRand, HP_REGEN_MAX, tier - 1)
                MP_REGEN_MIN -> modMap[MP_REGEN_MAX] = modStrength(mtRand, MP_REGEN_MAX, tier - 1)
                else -> Unit
            }
        }
        return FuturisticClothing(
            "$adjective $quality$material $shirtName",
            "jw_shirt$imageNum.gif",
            modMap,
        )
    }

    fun hat(day: Int, tier: Int): FuturisticClothing {
        val seed = day * 11392L + 2063L
        val mtRand = PHPMTRandom(seed)
        val rand = PHPRandom(seed)
        val imageNum = mtRand.nextInt(1, 9)

        mtRand.nextDouble()
        mtRand.nextDouble()
        mtRand.nextDouble()
        val adjective = mtRand.pickOne(hatAdjectives)
        mtRand.nextDouble()
        val mat1 = mtRand.pickOne(hatMaterials)
        mtRand.nextDouble()
        var mat2 = mat1
        while (mat1 == mat2) {
            mat2 = mtRand.pickOne(hatMaterials)
        }
        val emphasis = mtRand.pickOne(hatEmphasis)
        val hatName = mtRand.pickOne(hats)

        val hatModifiers = mutableListOf(
            MUS, MYS, MOX, HP, MP, HP_REGEN_MIN, MP_REGEN_MIN,
            HOT_DAMAGE, COLD_DAMAGE, STENCH_DAMAGE, SLEAZE_DAMAGE, SPOOKY_DAMAGE,
            HOT_SPELL_DAMAGE, COLD_SPELL_DAMAGE, STENCH_SPELL_DAMAGE, SLEAZE_SPELL_DAMAGE, SPOOKY_SPELL_DAMAGE,
        )
        if (tier > 3) {
            hatModifiers.addAll(listOf(ITEMDROP, MEATDROP, MONSTER_LEVEL))
        }
        rand.shuffle(hatModifiers)
        val modMap = linkedMapOf<DoubleModifier, Int>()
        for (i in 0 until tier) {
            val mod = hatModifiers[i]
            modMap[mod] = modStrength(mtRand, mod, tier - 1)
            when (mod) {
                HP_REGEN_MIN -> modMap[HP_REGEN_MAX] = modStrength(mtRand, HP_REGEN_MAX, tier - 1)
                MP_REGEN_MIN -> modMap[MP_REGEN_MAX] = modStrength(mtRand, MP_REGEN_MAX, tier - 1)
                else -> Unit
            }
        }
        return FuturisticClothing(
            "$adjective $mat1-$mat2 $emphasis$hatName",
            "jw_hat$imageNum.gif",
            modMap,
        )
    }

    fun collar(day: Int, tier: Int): FuturisticClothing {
        val seed = day * 11393L + 2063L
        val mtRand = PHPMTRandom(seed)
        val rand = PHPRandom(seed)
        val imageNum = mtRand.nextInt(1, 9)

        mtRand.nextDouble()
        val adjective = mtRand.pickOne(collarAdjectives)
        mtRand.nextDouble()
        val and = if (mtRand.nextInt(0, 1) == 0) "-" else " and "
        val color1 = mtRand.pickOne(collarColors)
        mtRand.nextDouble()
        var color2 = color1
        while (color1 == color2) {
            color2 = mtRand.pickOne(collarColors)
        }

        val collarModifiers = mutableListOf(FAMILIAR_WEIGHT, FAMILIAR_DAMAGE)
        if (tier > 3) collarModifiers.add(FAMILIAR_EXP)
        rand.shuffle(collarModifiers)
        val mod = collarModifiers[0]
        val modMap = linkedMapOf(mod to modStrength(mtRand, mod, tier - 1))
        return FuturisticClothing(
            "$adjective $color1$and$color2 ${collars[(imageNum - 1) / 3]}",
            "jw_pet$imageNum.gif",
            modMap,
        )
    }

    private fun modStrength(mtRand: PHPMTRandom, mod: DoubleModifier, level: Int): Int = when (mod) {
        MUS, MYS, MOX -> mtRand.nextInt(10 * level + 10, 12 * level + 12)
        HOT_RESISTANCE, COLD_RESISTANCE, STENCH_RESISTANCE, SPOOKY_RESISTANCE, SLEAZE_RESISTANCE ->
            mtRand.nextInt(level + 1, level + 3)
        HP, MP -> mtRand.nextInt(20 * level + 10, 20 * level + 30)
        HP_REGEN_MIN -> mtRand.nextInt(2 * level + 2, 2 * level + 4)
        MP_REGEN_MIN -> mtRand.nextInt(3 * level + 3, 3 * level + 5)
        HP_REGEN_MAX, MP_REGEN_MAX -> mtRand.nextInt(5 * level + 5, 5 * level + 10)
        DAMAGE_REDUCTION -> mtRand.nextInt(3 * level + 1, 3 * level + 5)
        DAMAGE_ABSORPTION -> mtRand.nextInt(10 * level + 10, 15 * level + 15)
        ITEMDROP -> mtRand.nextInt(5 * level + 3, 5 * level + 8)
        MEATDROP -> mtRand.nextInt(10 * level + 5, 10 * level + 15)
        MONSTER_LEVEL -> mtRand.nextInt(5 * level, 5 * level + 10)
        HOT_DAMAGE, COLD_DAMAGE, STENCH_DAMAGE, SPOOKY_DAMAGE, SLEAZE_DAMAGE,
        HOT_SPELL_DAMAGE, COLD_SPELL_DAMAGE, STENCH_SPELL_DAMAGE, SPOOKY_SPELL_DAMAGE, SLEAZE_SPELL_DAMAGE ->
            mtRand.nextInt(4 * level + 4, 6 * level + 6)
        FAMILIAR_WEIGHT -> 7 + 2 * level - mtRand.nextInt(0, 2)
        FAMILIAR_DAMAGE -> mtRand.nextInt(15 * level + 15, 25 * level + 25)
        FAMILIAR_EXP -> level + 1
        else -> 0
    }
}
