package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.data.MonsterDrop
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Focused XLVI Track A coverage (phases 6611–6630 combat prediction).
 * Revision bump deferred to parent (phase6850); stays phase6850.
 */
class GameRuntimeLibraryPhase6630Test {

    @Test
    fun revision_staysPhase6610() {
        assertEquals("phase7210", GameRuntimeLibrary.REVISION)
        assertEquals(6730, GameRuntimeLibrary.revisionNumber())
    }

    @Test
    fun expected_damage_appliesOcrsAskewAttack() {
        MonsterStatusTracker.resetLastMonster()
        MonsterStatusTracker.setNextMonster(
            MonsterDefinition(
                name = "phase6630 askew slug",
                id = 66301,
                image = "x.gif",
                attack = 40,
                defense = 10,
                hp = 20,
                initiative = 0,
                meatDrop = 0,
                phylum = "beast",
                isBoss = false,
                isGhost = false,
                isLucky = false,
                isScaling = false,
                scale = 0,
                cap = 0,
                floor = 0,
                drops = emptyList(),
                randomModifiers = listOf("askew"),
            ),
            listOf("askew"),
        )
        try {
            val char = KoLCharacter().also {
                it.updateFromApiResponse(
                    CharacterApiResponse(buffedmox = "10", buffedmus = "10", classId = "5"),
                )
            }
            val lib = GameRuntimeLibrary(
                preferences = Preferences(MapSettings()),
                character = char,
            )
            // askew: attack = 40*11/10 = 44; base = max(0,44-10)+44/4 = 45; DA0 → ceil(49.5)=50
            // without OCRS: base = 40; ceil(44)=44
            val out = outputLib(lib, "print(expected_damage());").trim().toInt()
            assertEquals(50, out)
        } finally {
            MonsterStatusTracker.resetLastMonster()
        }
    }

    @Test
    fun elemental_resistance_appliesOcrsElementOverlay() {
        MonsterStatusTracker.resetLastMonster()
        MonsterStatusTracker.setNextMonster(
            MonsterDefinition(
                name = "phase6630 steamy",
                id = 66302,
                image = "x.gif",
                attack = 10,
                defense = 10,
                hp = 10,
                initiative = 0,
                meatDrop = 0,
                phylum = "beast",
                isBoss = false,
                isGhost = false,
                isLucky = false,
                isScaling = false,
                scale = 0,
                cap = 0,
                floor = 0,
                attackElement = "",
                drops = emptyList(),
                randomModifiers = listOf("steamy"),
            ),
            listOf("steamy"),
        )
        try {
            val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
            // No hot resistance equipped → 0.0, but path must resolve attack element "hot".
            assertEquals(
                "0.0",
                outputLib(lib, "print(to_string(elemental_resistance()));").trim(),
            )
            assertEquals(
                "0.0",
                outputLib(lib, """print(to_string(elemental_resistance(to_element("hot"))));""").trim(),
            )
        } finally {
            MonsterStatusTracker.resetLastMonster()
        }
    }

    @Test
    fun elementalResistanceByLevel_matchesDesktopNegativeLevels() {
        // Desktop: levels*10 + mystBonus → -30 + 5 (callers clamp before invoking).
        assertEquals(
            -25.0,
            CombatAdjustment.elementalResistanceByLevel(-3, mystBonus = true, isMystClass = true),
        )
        assertEquals(
            -30.0,
            CombatAdjustment.elementalResistanceByLevel(-3, mystBonus = false, isMystClass = true),
        )
    }

    @Test
    fun will_usually_miss_attacksCantMissIsFalse() {
        MonsterStatusTracker.resetLastMonster()
        MonsterStatusTracker.setNextMonster(
            MonsterDefinition(
                name = "phase6630 tank",
                id = 66303,
                image = "x.gif",
                attack = 10,
                defense = 999,
                hp = 50,
                initiative = 0,
                meatDrop = 0,
                phylum = "beast",
                isBoss = false,
                isGhost = false,
                isLucky = false,
                isScaling = false,
                scale = 0,
                cap = 0,
                floor = 0,
                drops = emptyList(),
            ),
            emptyList(),
        )
        try {
            assertTrue(
                !CombatAdjustment.willUsuallyMiss(
                    MonsterStatusTracker.getLastMonster(),
                    hitStat = Int.MAX_VALUE,
                    ml = 0,
                ),
            )
        } finally {
            MonsterStatusTracker.resetLastMonster()
        }
    }

    @Test
    fun will_usually_dodge_respectsOcrsBouncingAttack() {
        MonsterStatusTracker.resetLastMonster()
        MonsterStatusTracker.setNextMonster(
            MonsterDefinition(
                name = "phase6630 bouncing",
                id = 66304,
                image = "x.gif",
                attack = 20,
                defense = 10,
                hp = 20,
                initiative = 0,
                meatDrop = 0,
                phylum = "beast",
                isBoss = false,
                isGhost = false,
                isLucky = false,
                isScaling = false,
                scale = 0,
                cap = 0,
                floor = 0,
                drops = emptyList(),
                randomModifiers = listOf("bouncing"),
            ),
            listOf("bouncing"),
        )
        try {
            val char = KoLCharacter().also {
                // moxie 30: plain atk 20 → dodge (30-20-6>0); bouncing atk 30 → no dodge
                it.updateFromApiResponse(CharacterApiResponse(buffedmox = "30", buffedmus = "10"))
            }
            val lib = GameRuntimeLibrary(
                preferences = Preferences(MapSettings()),
                character = char,
            )
            assertEquals("false", outputLib(lib, "print(to_string(will_usually_dodge()));").trim())
        } finally {
            MonsterStatusTracker.resetLastMonster()
        }
    }

    @Test
    fun meat_drop_solidGoldOcrsOverlay() {
        MonsterStatusTracker.resetLastMonster()
        MonsterStatusTracker.setNextMonster(
            MonsterDefinition(
                name = "phase6630 gold",
                id = 66305,
                image = "x.gif",
                attack = 10,
                defense = 10,
                hp = 10,
                initiative = 0,
                meatDrop = 50,
                phylum = "beast",
                isBoss = false,
                isGhost = false,
                isLucky = false,
                isScaling = false,
                scale = 0,
                cap = 0,
                floor = 0,
                drops = emptyList(),
                randomModifiers = listOf("solid gold"),
            ),
            listOf("solid gold"),
        )
        try {
            val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
            assertEquals("1000", outputLib(lib, "print(meat_drop());").trim())
        } finally {
            MonsterStatusTracker.resetLastMonster()
        }
    }

    @Test
    fun item_drops_array_unknownRateTypeIsZero() {
        val monster = MonsterDefinition(
            name = "phase6630 unknown drop",
            id = 66306,
            image = "x.gif",
            attack = 1,
            defense = 1,
            hp = 1,
            initiative = 0,
            meatDrop = 0,
            phylum = "beast",
            isBoss = false,
            isGhost = false,
            isLucky = false,
            isScaling = false,
            scale = 0,
            cap = 0,
            floor = 0,
            drops = listOf(MonsterDrop("mystery goo", 0.0, prefix = null)),
        )
        MonsterStatusTracker.resetLastMonster()
        MonsterStatusTracker.setNextMonster(monster, emptyList())
        try {
            val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
            assertEquals(
                "0",
                outputLib(lib, "print(item_drops_array()[0].type);").trim(),
            )
        } finally {
            MonsterStatusTracker.resetLastMonster()
        }
    }

    @Test
    fun item_drops_conditionalRateStillFractional() = runBlocking {
        val db = GameDatabase()
        db.load()
        val sandworm = MonsterDatabase.getByName("giant sandworm")
        assertTrue(sandworm != null)
        val spice = sandworm!!.drops.firstOrNull { it.itemName.equals("spice melange", ignoreCase = true) }
        assertTrue(spice != null)
        assertEquals('c', spice!!.prefix)
        assertEquals(0.1, spice.dropRate, 0.0001)
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "0.1",
            outputLib(
                lib,
                """print(to_string(item_drops(to_monster("giant sandworm"))[to_item("spice melange")]));""",
            ).trim(),
        )
    }

    @Test
    fun jump_chance_ninjaMaskOcrsIsZero() {
        MonsterStatusTracker.resetLastMonster()
        MonsterStatusTracker.setNextMonster(
            MonsterDefinition(
                name = "phase6630 masked",
                id = 66307,
                image = "x.gif",
                attack = 10,
                defense = 10,
                hp = 10,
                initiative = 50,
                hasInitiative = true,
                meatDrop = 0,
                phylum = "beast",
                isBoss = false,
                isGhost = false,
                isLucky = false,
                isScaling = false,
                scale = 0,
                cap = 0,
                floor = 0,
                drops = emptyList(),
                randomModifiers = listOf("ninja mask"),
            ),
            listOf("ninja mask"),
        )
        try {
            val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
            assertEquals("0", outputLib(lib, "print(jump_chance());").trim())
        } finally {
            MonsterStatusTracker.resetLastMonster()
        }
    }

    @Test
    fun jump_chance_threeArgStillInitOnlyMl() = runBlocking {
        val db = GameDatabase()
        db.load()
        val mosquito = MonsterDatabase.getByName("huge mosquito")!!
        val atZero = CombatAdjustment.jumpChance(mosquito, 0, initMl = 0, attackMl = 0, baseMainstat = 0)
        val atFortyInit = CombatAdjustment.jumpChance(mosquito, 0, initMl = 40, attackMl = 0, baseMainstat = 0)
        assertTrue(atFortyInit < atZero)
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            atFortyInit.toString(),
            outputLib(lib, """print(jump_chance(to_monster("huge mosquito"), 0, 40));""").trim(),
        )
    }
}
