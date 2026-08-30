package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop BadMoonManager headless port (Phases 3321–3335). */
object BadMoonManager {

    enum class Type(val label: String) {
        STAT1("+20 to one stat, -5 to others"),
        STAT2("+40 to one stat, -50% Familiar Weight"),
        STAT3("+50% to one stat, -50% to another"),
        DAMAGE1("+10 damage, Damage Reduction -2"),
        DAMAGE2("+20 damage, 1-3 damage/round to self"),
        RESIST1("So-So resistance to one, Vulnerability to opposites"),
        RESIST2("Resistance to all, -attributes"),
        ITEM_DROP("Item Drop"),
        MEAT_DROP("Meat Drop"),
        DAMAGE_REDUCTION("+ Damage Reduction, - Weapon Damage"),
        MEAT("Meat"),
        ITEMS("Items"),
    }

    data class Encounter(
        val name: String,
        val zone: String,
        val prereqs: String?,
        val effectName: String?,
        val description: String,
        val type: Type,
        val setting: String,
    )

    val SPECIAL_ENCOUNTERS: Array<Encounter> = arrayOf(
        Encounter("O Goblin, Where Art Thou?", "Outskirts of Cobb's Knob", "receiving Knob Goblin Encryption Key", "Minioned", "Muscle +20, Mysticality -5, Moxie -5", Type.STAT1, "badMoonEncounter01"),
        Encounter("Pantry Raid!", "The Haunted Pantry", "opening Spookyraven Manor", "Enhanced Archaeologist", "Mysticality +20, Muscle -5, Moxie -5", Type.STAT1, "badMoonEncounter02"),
        Encounter("Sandwiched in the Club", "The Sleazy Back Alley", null, "Chronologically Pummeled", "Moxie +20, Muscle -5, Mysticality -5", Type.STAT1, "badMoonEncounter03"),
        Encounter("It's So Heavy", "Cobb's Knob Treasury", null, "Animal Exploiter", "Muscle +40, Familiar Weight -50%", Type.STAT2, "badMoonEncounter04"),
        Encounter("KELF! I Need Somebody!", "Cobb's Knob Kitchens", null, "Scent of a Kitchen Elf", "Mysticality +40, Familiar Weight -50%", Type.STAT2, "badMoonEncounter05"),
        Encounter("On The Whole, the Bark is Better", "Cobb's Knob Harem", null, "Once Bitten, Twice Shy", "Moxie +40, Familiar Weight -50%", Type.STAT2, "badMoonEncounter06"),
        Encounter("It's All The Rage", "The Orcish Frat House", null, "The Rage", "Muscle +50%, Mysticality -50%", Type.STAT3, "badMoonEncounter07"),
        Encounter("Double-Secret Initiation", "The Orcish Frat House (In Disguise)", null, "Shamed and Manipulated", "Muscle +50%, Moxie -50%", Type.STAT3, "badMoonEncounter08"),
        Encounter("Better Dread Than Dead", "The Hippy Camp", null, "Dreadlocked", "Mysticality +50%, Moxie -50%", Type.STAT3, "badMoonEncounter09"),
        Encounter("Drumroll, Please", "The Hippy Camp (In Disguise)", null, "Drummed Out", "Mysticality +50%, Muscle -50%", Type.STAT3, "badMoonEncounter10"),
        Encounter("How Far Down Do You Want To Go?", "The Obligatory Pirate's Cove (Undisguised)", null, "Hornswaggled", "Moxie +50%, Muscle -50%", Type.STAT3, "badMoonEncounter11"),
        Encounter("Mind Your Business", "The Obligatory Pirate's Cove (In Disguise)", null, "Third Eye Blind", "Moxie +50%, Mysticality -50%", Type.STAT3, "badMoonEncounter12"),
        Encounter("Vole Call!", "The Haunted Billiards Room", "opening Haunted Library", "Re-Possessed", "Bonus Weapon Damage +10, Damage Reduction -2", Type.DAMAGE1, "badMoonEncounter13"),
        Encounter("Frost Bitten, Twice Shy", "The Goatlet", "opening The eXtreme Slope", "Frostbitten", "+10 Cold Damage, Damage Reduction -2", Type.DAMAGE1, "badMoonEncounter14"),
        Encounter("If You Smell Something Burning, It's My Heart", "The Haunted Kitchen", null, "Burning Heart", "+10 Hot Damage, Damage Reduction -2", Type.DAMAGE1, "badMoonEncounter15"),
        Encounter("Oil Be Seeing You", "Pandamonium Slums", "completed Azazel Quest", "Basted", "+10 Sleaze Damage, Damage Reduction -2", Type.DAMAGE1, "badMoonEncounter16"),
        Encounter("Back Off, Man. I'm a Scientist.", "The Haunted Library", null, "Freaked Out", "+10 Spooky Damage, Damage Reduction -2", Type.DAMAGE1, "badMoonEncounter17"),
        Encounter("Oh Guanoes!", "Guano Junction", null, "Guanified", "+10 Stench Damage, Damage Reduction -2", Type.DAMAGE1, "badMoonEncounter18"),
        Encounter("Do You Think You're Better Off Alone", "The Castle in the Clouds in the Sky", "completed Giant Trash Quest", "Raving Lunatic", "Melee Damage +20, Lose 1-3 HP per combat round", Type.DAMAGE2, "badMoonEncounter19"),
        Encounter("The Big Chill", "The Icy Peak", null, "Hyperbolic Hypothermia", "+20 Cold Damage, Lose 1-3 HP (cold damage) per combat round", Type.DAMAGE2, "badMoonEncounter20"),
        Encounter("Mr. Sun Is Not Your Friend", "An Oasis", "receiving worm-riding hooks", "Solar Flair", "+20 Hot Damage, Lose 1-3 HP (hot damage) per combat round", Type.DAMAGE2, "badMoonEncounter21"),
        Encounter("Pot Jacked", "The Hole in the Sky", "made Richard's star key", "Greased", "+20 Sleaze Damage, Lose 1-3 HP (sleaze damage) per combat round", Type.DAMAGE2, "badMoonEncounter22"),
        Encounter("Party Crasher", "The Haunted Ballroom", "opening Haunted Wine Cellar", "Slimed", "+20 Spooky Damage, Lose 1-3 HP (spooky damage) per combat round", Type.DAMAGE2, "badMoonEncounter23"),
        Encounter("A Potentially Offensive Reference Has Been Carefully Avoided Here", "The Black Forest", "opening The Black Market", "Tar Struck", "+20 Stench Damage, Lose 1-3 HP (stench damage) per combat round", Type.DAMAGE2, "badMoonEncounter24"),
        Encounter("Strategy: Get Arts", "Inside the Palindome", "defeating Dr. Awkward", "Paw Swap", "So-So Cold Resistance. Double damage from Hot and Spooky", Type.RESIST1, "badMoonEncounter25"),
        Encounter("Pot-Unlucky", "The Hidden City", "opening A Smallish Temple", "Deep Fried", "So-So Hot Resistance, Double damage from Stench and Sleaze", Type.RESIST1, "badMoonEncounter26"),
        Encounter("Mistaken Identity, LOL", "The Valley of Rof L'm Fao", "receiving facsimile dictionary", "Scared Stiff", "So-So Sleaze Resistance, Double damage from Cold and Spooky", Type.RESIST1, "badMoonEncounter27"),
        Encounter("Mind the Fine Print", "Tower Ruins", null, "Side Affectation", "So-So Spooky Resistance, Double damage from Stench and Hot", Type.RESIST1, "badMoonEncounter28"),
        Encounter("Sweatin' Like a Vet'ran", "The Arid, Extra-Dry Desert (Ultrahydrated)", "receiving worm-riding hooks", "Shirtless in Seattle", "So-So Stench Resistance, Double damage from Cold and Sleaze", Type.RESIST1, "badMoonEncounter29"),
        Encounter("Elementally, My Deal Watson", "Beanbat Chamber", "opening The Beanstalk", "Batigue", "Slight Resistance to All Elements, All Attributes -10%", Type.RESIST2, "badMoonEncounter30"),
        Encounter("Hair of the Hellhound", "The Haunted Wine Cellar", "defeating Lord Spookyraven", "Cupshotten", "So-So Resistance to All Elements, All Attributes -20%", Type.RESIST2, "badMoonEncounter31"),
        Encounter("Shall We Dance", "Cobb's Knob Laboratory", null, "The Vitus Virus", "+50% Items from Monsters, -5 Stats Per Fight", Type.ITEM_DROP, "badMoonEncounter32"),
        Encounter("You Look Flushed", "The Haunted Bathroom", null, "Your Number 1 Problem", "+100% Items from Monsters, All Attributes -20", Type.ITEM_DROP, "badMoonEncounter33"),
        Encounter("What Do We Want?", "The Misspelled Cemetary (Pre-Cyrpt)", null, "Braaaaains", "+50% Meat from Monsters, -50% Combat Initiative", Type.MEAT_DROP, "badMoonEncounter34"),
        Encounter("When Do We Want It?", "The Misspelled Cemetary (Post-Cyrpt)", null, "Braaaaaaaains", "+200% Meat from Monsters, -50% Items from Monsters", Type.MEAT_DROP, "badMoonEncounter35"),
        Encounter("Getting Hammered", "The Inexplicable Door", "receiving digital key", "Midgetized", "Damage Reduction: 4. Weapon Damage -8", Type.DAMAGE_REDUCTION, "badMoonEncounter36"),
        Encounter("Obligatory Mascot Cameo", "The Penultimate Fantasy Airship", "opening The Castle in the Clouds in the Sky", "Synthesized", "Damage Reduction: 8, Weapon Damage -8", Type.DAMAGE_REDUCTION, "badMoonEncounter37"),
        Encounter("This Doesn't Look Like Candy Mountain", "The Spooky Forest", null, "Missing Kidney", "1,000 Meat", Type.MEAT, "badMoonEncounter38"),
        Encounter("Flowers For ", "Degrassi Knoll", "returned the bitchin' meatcar to the guild.", "Duhhh", "2,000 Meat, Lose 12-56(?) MP, Mysticality -20", Type.MEAT, "badMoonEncounter39"),
        Encounter("Onna Stick", "The Bat Hole Entrance", "opening The Boss Bat's Lair", "Affronted Decency", "3,000 Meat, Moxie -20", Type.MEAT, "badMoonEncounter40"),
        Encounter("The Beaten-Senseless Man's Hand", "South of The Border", null, "Beaten Up", "4,000 Meat, All Attributes -50%", Type.MEAT, "badMoonEncounter41"),
        Encounter("A White Lie", "Whitey's Grove", "opening The Road to the White Citadel", "Maid Disservice", "5,000 Meat, All Attributes -20%", Type.MEAT, "badMoonEncounter42"),
        Encounter("Surprising!", "Noob Cave", null, null, "Familiar-Gro™ Terrarium, black kitten, 14 Drunkenness", Type.ITEMS, "badMoonEncounter43"),
        Encounter("That's My Favorite Kind of Contraption", "The Spooky Forest", "opening The Hidden Temple", "Dang Near Cut in Half", "Muscle -50%, Gain Torso Awareness", Type.ITEMS, "badMoonEncounter44"),
        Encounter("Say Cheese!", "The Arid, Extra-Dry Desert (unhydrated)", null, null, "anticheese, lose 50 HP", Type.ITEMS, "badMoonEncounter45"),
        Encounter("Because Stereotypes Are Awesome", "The Typical Tavern (Post-Quest)", null, null, "leprechaun hatchling, 1 Drunkenness", Type.ITEMS, "badMoonEncounter46"),
        Encounter("Why Did It Have To Be Snake Eyes?", "The Hidden Temple", null, null, "loaded dice", Type.ITEMS, "badMoonEncounter47"),
        Encounter("The Placebo Defect", "The Haunted Conservatory", null, null, "potato sprout, Lose 75% HP & MP", Type.ITEMS, "badMoonEncounter48"),
    )

    fun inBadMoon(state: CharacterState?): Boolean =
        ZodiacSign.find(state?.zodiacSign.orEmpty())?.isBadMoon == true

    fun encounterForName(encounterName: String): Encounter? =
        SPECIAL_ENCOUNTERS.firstOrNull { encounterName.startsWith(it.name) }

    fun validateBadMoon(preferences: Preferences?, ascensionNumber: Int) {
        if (preferences == null) return
        val lastAscension = preferences.getInt("lastBadMoonReset", -1)
        if (lastAscension < ascensionNumber) {
            preferences.setInt("lastBadMoonReset", ascensionNumber)
            SPECIAL_ENCOUNTERS.forEach { preferences.setBoolean(it.setting, false) }
        }
    }

    fun specialAdventure(encounterName: String, state: CharacterState?): Boolean {
        if (!inBadMoon(state)) return false
        return encounterForName(encounterName) != null
    }

    fun registerAdventure(encounterName: String, preferences: Preferences?, ascensionNumber: Int = 0) {
        val data = encounterForName(encounterName) ?: return
        validateBadMoon(preferences, ascensionNumber)
        preferences?.setBoolean(data.setting, true)
    }

    fun haveEncounter(setting: String, preferences: Preferences?): Boolean =
        preferences?.getBoolean(setting, false) == true

    fun report(print: (String) -> Unit, preferences: Preferences?, ascensionNumber: Int = 0) {
        validateBadMoon(preferences, ascensionNumber)
        print("Bad Moon special encounters")
        print("Type | Have | Name | Location | Reward")
        Type.entries.forEach { type ->
            val rows = SPECIAL_ENCOUNTERS.filter { it.type == type }
            if (rows.isEmpty()) return@forEach
            print("--- ${type.label} ---")
            rows.forEach { row ->
                val have = haveEncounter(row.setting, preferences)
                val location = buildString {
                    append(row.zone)
                    if (!row.prereqs.isNullOrBlank()) append(" after ${row.prereqs}")
                }
                val reward = buildString {
                    row.effectName?.let { append("$it: ") }
                    append(row.description)
                }
                print("${if (have) "yes" else "no"} | ${row.name} | $location | $reward")
            }
        }
    }

    fun completedCount(preferences: Preferences?): Int =
        SPECIAL_ENCOUNTERS.count { haveEncounter(it.setting, preferences) }
}
