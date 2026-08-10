package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP57Test {

    @Test
    fun numericPhys_ancientProtectorSpirit() = runBlocking {
        MonsterDatabase.load()
        val aps = MonsterDatabase.getByName("ancient protector spirit")!!
        assertEquals(100, aps.physicalResistance)
        assertEquals(100, CombatAdjustment.monsterPhysicalResistance(aps))
        assertEquals(0, CombatAdjustment.monsterElementalResistance(aps))
    }

    @Test
    fun numericElem_blueOysterCultist() = runBlocking {
        MonsterDatabase.load()
        val cultist = MonsterDatabase.getByName("Blue Oyster cultist")!!
        assertEquals(75, cultist.elementalResistance)
        assertEquals(75, CombatAdjustment.monsterElementalResistance(cultist))
        // Per-element falls back to Elem when sub-attr absent
        assertEquals(75, CombatAdjustment.monsterElementResistance(cultist, "hot"))
    }

    @Test
    fun oilBaron_bothResistances() = runBlocking {
        MonsterDatabase.load()
        val baron = MonsterDatabase.getByName("oil baron")!!
        assertEquals(50, CombatAdjustment.monsterPhysicalResistance(baron))
        assertEquals(50, CombatAdjustment.monsterElementalResistance(baron))
    }

    @Test
    fun axisArtillery_perElementNoElemFallback() = runBlocking {
        MonsterDatabase.load()
        val skeleton = MonsterDatabase.getByName("Axis artillery skeleton")!!
        assertEquals(50, CombatAdjustment.monsterElementResistance(skeleton, "hot"))
        assertEquals(25, CombatAdjustment.monsterElementResistance(skeleton, "cold"))
        assertEquals(90, CombatAdjustment.monsterElementResistance(skeleton, "stench"))
        assertEquals(90, CombatAdjustment.monsterElementResistance(skeleton, "spooky"))
        assertEquals(50, CombatAdjustment.monsterElementResistance(skeleton, "sleaze"))
        assertEquals(0, CombatAdjustment.monsterElementalResistance(skeleton))
    }

    @Test
    fun shadowBat_elemExpressionPref() = runBlocking {
        MonsterDatabase.load()
        val bat = MonsterDatabase.getByName("shadow bat")!!
        assertEquals(100, CombatAdjustment.monsterPhysicalResistance(bat))
        val ctx0 = ExpressionContext(prefLookup = { if (it == "_shadowRiftCombats") "0" else "" })
        assertEquals(0, CombatAdjustment.monsterElementalResistance(bat, ctx0))
        val ctx40 = ExpressionContext(prefLookup = { if (it == "_shadowRiftCombats") "40" else "" })
        assertEquals(40, CombatAdjustment.monsterElementalResistance(bat, ctx40))
        val ctx100 = ExpressionContext(prefLookup = { if (it == "_shadowRiftCombats") "100" else "" })
        assertEquals(90, CombatAdjustment.monsterElementalResistance(bat, ctx100))
    }

    @Test
    fun godLobster_physElemEquipped() = runBlocking {
        MonsterDatabase.load()
        val lobster = MonsterDatabase.getByName("God Lobster")!!
        val bare = ExpressionContext()
        assertEquals(0, CombatAdjustment.monsterPhysicalResistance(lobster, bare))
        assertEquals(0, CombatAdjustment.monsterElementalResistance(lobster, bare))
        val crowned = ExpressionContext(
            equippedItemNames = setOf("god lobster's crown"),
        )
        // Phys: 80*equipped(Crown); Elem: 90*equipped(Crown)
        assertEquals(80, CombatAdjustment.monsterPhysicalResistance(lobster, crowned))
        assertEquals(90, CombatAdjustment.monsterElementalResistance(lobster, crowned))
    }

    @Test
    fun mosquito_absentResistancesAreZero() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "0",
            outputLib(lib, """print(to_monster("huge mosquito")["physical_resistance"]);""").trim(),
        )
        assertEquals(
            "0",
            outputLib(lib, """print(to_monster("huge mosquito")["elemental_resistance"]);""").trim(),
        )
        assertEquals(
            "0",
            outputLib(lib, """print(to_monster("huge mosquito")["hot_resistance"]);""").trim(),
        )
    }

    @Test
    fun ash_brackets_numericAndShadowBat() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString("_shadowRiftCombats", "25")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(
            "100",
            outputLib(lib, """print(to_monster("ancient protector spirit")["physical_resistance"]);""").trim(),
        )
        assertEquals(
            "75",
            outputLib(lib, """print(to_monster("Blue Oyster cultist")["elemental_resistance"]);""").trim(),
        )
        assertEquals(
            "25",
            outputLib(lib, """print(to_monster("shadow bat")["elemental_resistance"]);""").trim(),
        )
        assertEquals(
            "50",
            outputLib(lib, """print(to_monster("Axis artillery skeleton")["hot_resistance"]);""").trim(),
        )
    }

    @Test
    fun ash_godLobster_withCrown() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter()
        char.updateEquipment(EquipmentSlot.HAT, "God Lobster's Crown")
        val lib = GameRuntimeLibrary(gameDatabase = db, character = char)
        assertEquals(
            "80",
            outputLib(lib, """print(to_monster("God Lobster")["physical_resistance"]);""").trim(),
        )
        assertEquals(
            "90",
            outputLib(lib, """print(to_monster("God Lobster")["elemental_resistance"]);""").trim(),
        )
    }

    @Test
    fun revision_isPhase99() {
        assertEquals("phase400", GameRuntimeLibrary.REVISION)
        assertTrue(GameRuntimeLibrary.REVISION.startsWith("phase"))
    }
}
