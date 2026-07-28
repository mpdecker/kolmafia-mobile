package net.sourceforge.kolmafia.data

/**
 * Builds desktop-compatible craft type descriptions from concoctions.txt method tokens.
 * Mirrors desktop [ConcoctionDatabase.addCraftingData] + [mixingMethodDescription].
 */
object CraftTypeDescription {

    private enum class PrimaryType(val label: String) {
        COMBINE("Meatpasting"),
        COOK("Cooking"),
        MIX("Mixing"),
        SMITH("Meatsmithing"),
        SSMITH("Meatsmithing (not Innabox)"),
        STILL("Nash Crosby's Still"),
        MALUS("Malus of Forethought"),
        JEWELRY("Jewelry-making pliers"),
        ROLLING_PIN("rolling pin/unrolling pin"),
        GNOME_TINKER("Supertinkering"),
        STAFF("Rodoric, the Staffcrafter"),
        SUSHI("sushi-rolling mat"),
        SINGLE_USE("single-use"),
        MULTI_USE("multi-use"),
        SEWER("chewing gum"),
        CRIMBO05("Crimbo Town Toy Factory (Crimbo 2005)"),
        CRIMBO06("Uncle Crimbo's Mobile Home (Crimboween 2006)"),
        CRIMBO07("Uncle Crimbo's Mobile Home (Crimbo 2007)"),
        CRIMBO12("Uncle Crimbo's Futuristic Trailer (Crimboku 2012)"),
        PHINEAS("Phineas"),
        COOK_FANCY("Cooking (fancy)"),
        MIX_FANCY("Mixing (fancy)"),
        ACOMBINE("Meatpasting (not untinkerable)"),
        COINMASTER("Coin Master purchase"),
        CLIPART("Summon Clip Art"),
        VYKEA("VYKEA"),
        FLOUNDRY("Clan Floundry"),
        TERMINAL("Source Terminal"),
        BARREL("shrine to the Barrel god"),
        WAX("globs of wax"),
        SPACEGATE("Spacegate Equipment Requisition"),
        NEWSPAPER("burning newspaper"),
        METEOROID("metal meteoroid"),
        SAUSAGE_O_MATIC("Kramco Sausage-o-Matic"),
        FANTASY_REALM("Fantasy Realm Welcome Center"),
        STILLSUIT("tiny stillsuit"),
        WOOL("grubby wool"),
        BURNING_LEAVES("Pile of Burning Leaves"),
        MAYAM("Mayam Calendar"),
        PHOTO_BOOTH("Clan Photo Booth"),
        TAKERSPACE("TakerSpace"),
    }

    private enum class Requirement(val suffix: String) {
        MALE("(males only)"),
        FEMALE("(females only)"),
        SSPD("(St. Sneaky Pete's Day only)"),
        HAMMER("(tenderizing hammer)"),
        GRIMACITE("(depleted Grimacite hammer)"),
        TORSO("(Torso Awareness)"),
        SUPER_MEATSMITHING("(Super-Advanced Meatsmithing)"),
        ARMORCRAFTINESS("(Armorcraftiness)"),
        ELDRITCH("(Eldritch Intellect)"),
        EXPENSIVE("(Really Expensive Jewelrycrafting)"),
        REAGENT("(Advanced Saucecrafting)"),
        WAY("(The Way of Sauce)"),
        DEEP_SAUCERY("(Deep Saucery)"),
        PASTA("(Pastamastery)"),
        TRANSNOODLE("(Transcendental Noodlecraft)"),
        TEMPURAMANCY("(Tempuramancy)"),
        PATENT("(Patent Medicine)"),
        AC("(Advanced Cocktailcrafting)"),
        SHC("(Superhuman Cocktailcrafting)"),
        SALACIOUS("(Salacious Cocktailcrafting)"),
        TIKI("(Tiki Mixology)"),
        NOBEE("(Unavailable in Beecore)"),
    }

    fun describe(methods: Set<String>): String {
        var primary: PrimaryType? = null
        val requirements = linkedSetOf<Requirement>()

        for (token in methods.sorted()) {
            when (token) {
                "COMBINE" -> primary = PrimaryType.COMBINE
                "COOK" -> primary = PrimaryType.COOK
                "MIX" -> primary = PrimaryType.MIX
                "SMITH" -> primary = PrimaryType.SMITH
                "SSMITH" -> primary = PrimaryType.SSMITH
                "STILL" -> primary = PrimaryType.STILL
                "MALUS" -> primary = PrimaryType.MALUS
                "JEWEL" -> primary = PrimaryType.JEWELRY
                "ROLL" -> primary = PrimaryType.ROLLING_PIN
                "TINKER" -> primary = PrimaryType.GNOME_TINKER
                "STAFF" -> primary = PrimaryType.STAFF
                "SUSHI" -> primary = PrimaryType.SUSHI
                "SUSE" -> primary = PrimaryType.SINGLE_USE
                "MUSE" -> primary = PrimaryType.MULTI_USE
                "SEWER" -> primary = PrimaryType.SEWER
                "CRIMBO05" -> primary = PrimaryType.CRIMBO05
                "CRIMBO06" -> primary = PrimaryType.CRIMBO06
                "CRIMBO07" -> primary = PrimaryType.CRIMBO07
                "CRIMBO12" -> primary = PrimaryType.CRIMBO12
                "PHINEAS" -> primary = PrimaryType.PHINEAS
                "COOK_FANCY" -> primary = PrimaryType.COOK_FANCY
                "MIX_FANCY" -> primary = PrimaryType.MIX_FANCY
                "ACOMBINE" -> primary = PrimaryType.ACOMBINE
                "CLIPART" -> primary = PrimaryType.CLIPART
                "VYKEA" -> primary = PrimaryType.VYKEA
                "TERMINAL" -> primary = PrimaryType.TERMINAL
                "BARREL" -> primary = PrimaryType.BARREL
                "WAX" -> primary = PrimaryType.WAX
                "SPACEGATE" -> primary = PrimaryType.SPACEGATE
                "NEWSPAPER" -> primary = PrimaryType.NEWSPAPER
                "METEOROID" -> primary = PrimaryType.METEOROID
                "SAUSAGE_O_MATIC" -> primary = PrimaryType.SAUSAGE_O_MATIC
                "FANTASY_REALM" -> primary = PrimaryType.FANTASY_REALM
                "STILLSUIT" -> primary = PrimaryType.STILLSUIT
                "WOOL" -> primary = PrimaryType.WOOL
                "BURNING_LEAVES" -> primary = PrimaryType.BURNING_LEAVES
                "MAYAM" -> primary = PrimaryType.MAYAM
                "PHOTO_BOOTH" -> primary = PrimaryType.PHOTO_BOOTH
                "TAKERSPACE" -> primary = PrimaryType.TAKERSPACE
                "PASTAMASTERY" -> {
                    primary = PrimaryType.COOK
                    requirements += Requirement.PASTA
                }
                "PASTA" -> {
                    primary = PrimaryType.COOK_FANCY
                    requirements += Requirement.PASTA
                }
                "TNOODLE", "TRANSNOODLE" -> {
                    primary = PrimaryType.COOK_FANCY
                    requirements += Requirement.TRANSNOODLE
                }
                "TEMPURA" -> {
                    primary = PrimaryType.COOK_FANCY
                    requirements += Requirement.TEMPURAMANCY
                }
                "WSMITH" -> {
                    primary = PrimaryType.SSMITH
                    requirements += Requirement.SUPER_MEATSMITHING
                }
                "ASMITH" -> {
                    primary = PrimaryType.SSMITH
                    requirements += Requirement.ARMORCRAFTINESS
                }
                "ACOCK" -> {
                    primary = PrimaryType.MIX_FANCY
                    requirements += Requirement.AC
                }
                "SCOCK" -> {
                    primary = PrimaryType.MIX_FANCY
                    requirements += Requirement.SHC
                }
                "SACOCK" -> {
                    primary = PrimaryType.MIX_FANCY
                    requirements += Requirement.SALACIOUS
                }
                "TIKI" -> {
                    primary = PrimaryType.MIX
                    requirements += Requirement.TIKI
                }
                "EJEWEL" -> {
                    primary = PrimaryType.JEWELRY
                    requirements += Requirement.EXPENSIVE
                }
                "SAUCE" -> {
                    primary = PrimaryType.COOK_FANCY
                    requirements += Requirement.REAGENT
                }
                "SSAUCE" -> {
                    primary = PrimaryType.COOK_FANCY
                    requirements += Requirement.WAY
                }
                "DSAUCE" -> {
                    primary = PrimaryType.COOK_FANCY
                    requirements += Requirement.DEEP_SAUCERY
                }
                "MALE" -> requirements += Requirement.MALE
                "FEMALE" -> requirements += Requirement.FEMALE
                "SSPD" -> requirements += Requirement.SSPD
                "HAMMER" -> requirements += Requirement.HAMMER
                "GRIMACITE" -> requirements += Requirement.GRIMACITE
                "TORSO" -> requirements += Requirement.TORSO
                "WEAPON" -> requirements += Requirement.SUPER_MEATSMITHING
                "ARMOR" -> requirements += Requirement.ARMORCRAFTINESS
                "ELDRITCH" -> requirements += Requirement.ELDRITCH
                "EXPENSIVE" -> requirements += Requirement.EXPENSIVE
                "REAGENT" -> requirements += Requirement.REAGENT
                "WAY" -> requirements += Requirement.WAY
                "DEEP" -> requirements += Requirement.DEEP_SAUCERY
                "TEMPURAMANCY" -> requirements += Requirement.TEMPURAMANCY
                "PATENT" -> requirements += Requirement.PATENT
                "AC" -> requirements += Requirement.AC
                "SHC" -> requirements += Requirement.SHC
                "SALACIOUS" -> requirements += Requirement.SALACIOUS
                "NOBEE" -> requirements += Requirement.NOBEE
                "SX3", "NODISCOVERY", "MANUAL" -> Unit
                else -> if (!token.startsWith("ROW")) {
                    // Unknown tokens are ignored for description (desktop logs only).
                }
            }
        }

        val base = primary?.label ?: return "[unknown method of creation]"
        if (requirements.isEmpty()) return base
        return buildString {
            append(base)
            for (req in requirements) {
                append(' ')
                append(req.suffix)
            }
        }
    }
}

fun ConcoctionDatabase.craftTypeForItem(name: String): String? =
    getByResult(name)?.craftTypeDescription()
