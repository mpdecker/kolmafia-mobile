package net.sourceforge.kolmafia.combat

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.familiar.FamiliarIds

object EncounterModifierPipeline {

    data class EncounterModifierContext(
        val familiarId: Int,
        val ascensionPath: AscensionPath,
    )

    private val MASK_PATTERN = Regex("""(.*?) wearing an? (.*?)ask""")

    private val DINO_TYPES = listOf(
        "archelon",
        "chicken",
        "dilophosaur",
        "flatusaurus",
        "ghostasaurus",
        "kachungasaur",
        "pterodactyl",
        "spikolodon",
        "velociraptor",
    )

    private val DINO_MODS = listOf(
        "carrion-eating",
        "chilling",
        "cold-blooded",
        "foul-smelling",
        "glass-shelled",
        "high-altitude",
        "hot-blooded",
        "mist-shrouded",
        "primitive",
        "slimy",
        "steamy",
        "supersonic",
        "swamp",
        "sweaty",
    )

    private val DINO_GLUTTONY = listOf(
        "that consumed",
        "that just ate",
        "that recently devoured ",
        "that swallowed the soul of",
    )

    /**
     * Port of desktop post-OCRS handlers in [AdventureRequest.extractMonster].
     * Mutates [modifiers] and returns the stripped encounter name.
     */
    fun applyPostOcrs(
        monsterName: String,
        modifiers: MutableList<String>,
        ctx: EncounterModifierContext,
    ): String {
        var name = handleIntergnat(monsterName, modifiers, ctx.familiarId)
        name = handleNuclearAutumn(name, modifiers, ctx.ascensionPath)
        name = handleMask(name, modifiers, ctx.ascensionPath)
        name = handleDinosaurs(name, modifiers, ctx.ascensionPath)
        name = handleHats(name, modifiers, ctx.ascensionPath)
        return name
    }

    private fun handleIntergnat(
        monsterName: String,
        modifiers: MutableList<String>,
        familiarId: Int,
    ): String {
        if (familiarId != FamiliarIds.INTERGNAT) return monsterName
        return when {
            monsterName.contains(" WITH BACON!!!") -> {
                modifiers.add("bacon")
                monsterName.replace(" WITH BACON!!!", "")
            }
            monsterName.contains("ELDRITCH HORROR ") -> {
                modifiers.add("eldritch")
                monsterName.replace("ELDRITCH HORROR ", "")
            }
            monsterName.contains(" NAMED NEIL") -> {
                modifiers.add("neil")
                monsterName.replace(" NAMED NEIL", "")
            }
            monsterName.contains(" WITH SCIENCE!") -> {
                modifiers.add("science")
                monsterName.replace(" WITH SCIENCE!", "")
            }
            monsterName.contains(" AND TESLA!") -> {
                modifiers.add("tesla")
                monsterName.replace(" AND TESLA!", "")
            }
            else -> monsterName
        }
    }

    private fun handleNuclearAutumn(
        monsterName: String,
        modifiers: MutableList<String>,
        path: AscensionPath,
    ): String {
        if (path != AscensionPath.NUCLEAR_AUTUMN && path != AscensionPath.NUCLEAR) {
            return monsterName
        }
        if (!monsterName.contains("mutant ")) return monsterName
        modifiers.add("mutant")
        return singleStringDelete(monsterName, "mutant ")
    }

    private fun handleMask(
        monsterName: String,
        modifiers: MutableList<String>,
        path: AscensionPath,
    ): String {
        if (path != AscensionPath.DISGUISES_DELIMIT) return monsterName
        val match = MASK_PATTERN.find(monsterName) ?: return monsterName
        modifiers.add(match.groupValues[2] + "ask")
        return match.groupValues[1]
    }

    private fun handleDinosaurs(
        monsterName: String,
        modifiers: MutableList<String>,
        path: AscensionPath,
    ): String {
        if (path != AscensionPath.DINOSAURS) return monsterName
        var name = monsterName
        for (type in DINO_TYPES) {
            if (name.contains(type)) {
                modifiers.add(type)
                name = singleStringDelete(name, type)
                break
            }
        }
        for (mod in DINO_MODS) {
            if (name.contains(mod)) {
                modifiers.add(mod)
                name = singleStringDelete(name, mod)
                break
            }
        }
        for (devour in DINO_GLUTTONY) {
            if (name.contains(devour)) {
                name = singleStringDelete(name, devour)
                break
            }
        }
        return name.trim()
    }

    private fun handleHats(
        monsterName: String,
        modifiers: MutableList<String>,
        path: AscensionPath,
    ): String {
        if (path != AscensionPath.HAT_TRICK) return monsterName
        val parts = monsterName.split(" wearing ", limit = 2)
        if (parts.size == 1) return monsterName
        val hats = parts[1].split(" and ")
        for (rawHat in hats) {
            var hat = rawHat
            if (hat.startsWith("an ")) hat = hat.substring(3)
            if (hat.startsWith("a ")) hat = hat.substring(2)
            modifiers.add(hat)
        }
        return parts[0]
    }

    private fun singleStringDelete(original: String, search: String): String {
        val index = original.indexOf(search)
        if (index == -1) return original
        return original.removeRange(index, index + search.length)
    }
}
