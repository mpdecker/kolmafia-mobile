package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP55Test {

    @Test
    fun amokPutty_scaleImpliedExperience() = runBlocking {
        MonsterDatabase.load()
        val putty = MonsterDatabase.getByName("amok putty")!!
        val ctx = ExpressionContext(buffedMuscle = 50, buffedMysticality = 40, buffedMoxie = 30)
        // mainstat 50 + scale 1 = 51, clamp Cap 89 Floor 6 → 51/8 = 6.375
        assertEquals(6.375, CombatAdjustment.monsterExperience(putty, ml = 0, ctx), absoluteTolerance = 1e-9)
        assertEquals(
            6.375 + 10.0 / 6.0,
            CombatAdjustment.monsterExperience(putty, ml = 10, ctx),
            absoluteTolerance = 1e-9,
        )
    }

    @Test
    fun guyMadeOfBees_numericExp() = runBlocking {
        MonsterDatabase.load()
        val bees = MonsterDatabase.getByName("Guy Made Of Bees")!!
        assertTrue(bees.hasExperience)
        assertEquals(40, bees.experience)
        assertEquals(20.0, CombatAdjustment.monsterExperience(bees, ml = 0), absoluteTolerance = 1e-9)
    }

    @Test
    fun conjoinedZmombie_expressionExp() = runBlocking {
        MonsterDatabase.load()
        val zmombie = MonsterDatabase.getByName("conjoined zmombie")!!
        assertEquals(
            26.5,
            CombatAdjustment.monsterExperience(
                zmombie,
                ml = 0,
                ExpressionContext(monsterLevel = 0),
            ),
            absoluteTolerance = 1e-9,
        )
        // Exp [53+ML/3] with ML token in expr context
        val ctx = ExpressionContext(monsterLevel = 6)
        assertEquals(
            (53.0 + 2.0) / 2.0,
            CombatAdjustment.monsterExperience(zmombie, ml = 6, ctx),
            absoluteTolerance = 1e-9,
        )
    }

    @Test
    fun baseMainstatExp_ashBracket() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "20.0",
            outputLib(lib, """print(to_monster("Guy Made Of Bees")["base_mainstat_exp"]);""").trim(),
        )
        assertEquals(
            "26.5",
            outputLib(lib, """print(to_monster("conjoined zmombie")["base_mainstat_exp"]);""").trim(),
        )
    }

    @Test
    fun garbageShirt_doublesExperience() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString("garbageShirtCharge", "3")
        val char = KoLCharacter()
        char.updateEquipment(EquipmentSlot.SHIRT, "makeshift garbage shirt")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char, preferences = prefs)
        assertEquals(
            "40.0",
            outputLib(lib, """print(to_monster("Guy Made Of Bees")["base_mainstat_exp"]);""").trim(),
        )
    }

    @Test
    fun mosquito_defaultExpAndBaseAttackRegression() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        // (16-0)/8 = 2.0
        assertEquals(
            "2.0",
            outputLib(lib, """print(to_monster("huge mosquito")["base_mainstat_exp"]);""").trim(),
        )
        assertEquals(
            "16",
            outputLib(lib, """print(to_monster("huge mosquito")["base_attack"]);""").trim(),
        )
        assertEquals(
            "51",
            outputLib(
                GameRuntimeLibrary(
                    gameDatabase = db,
                    character = KoLCharacter().also {
                        it.updateFromApiResponse(CharacterApiResponse(buffedmox = "50", mox = "50"))
                    },
                ),
                """print(monster_attack(to_monster("amok putty")));""",
            ).trim(),
        )
    }

    @Test
    fun revision_isPhase97() {
        assertEquals("phase190", GameRuntimeLibrary.REVISION)
        assertTrue(GameRuntimeLibrary.REVISION.startsWith("phase"))
    }
}
