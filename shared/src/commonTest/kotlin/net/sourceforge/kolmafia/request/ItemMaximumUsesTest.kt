package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.DailyLimitDatabase
import net.sourceforge.kolmafia.data.DailyLimitKind
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.preferences.Preferences

class ItemMaximumUsesTest {

    @BeforeTest
    fun setUp() = runTest {
        GameDatabase().load()
    }

    @Test
    fun food_usesFullnessRemaining() {
        val ctx = ctx(
            CharacterState(fullness = 10, fullnessLimit = 15),
        )
        assertEquals(5, maximumUses(471, "hot wing", ctx))
    }

    @Test
    fun drink_usesInebrietyRemaining() {
        val item = ItemDatabase.getByName("martini")!!
        val ctx = ctx(
            CharacterState(inebriety = 2, inebrietyLimit = 14),
        )
        assertEquals(4, maximumUses(item.id, item.name, ctx))
    }

    @Test
    fun spleen_usesDailyLimitWhenConfigured() {
        val item = ItemDatabase.getByName("turkey blaster")!!
        val ctx = ctx(
            CharacterState(spleenUsed = 0, spleenLimit = 15),
        )
        assertEquals(3, maximumUses(item.id, item.name, ctx))
    }

    @Test
    fun dailyUseLimit_exhaustedPrefReturnsZero() {
        val item = ItemDatabase.getByName("chester's bag of candy")!!
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("_bagOfCandyUsed", true)
        val ctx = ctx(CharacterState(), prefs)
        assertEquals(0, maximumUses(item.id, item.name, ctx))
    }

    @Test
    fun dailyUseLimit_availableWhenPrefUnset() {
        val item = ItemDatabase.getByName("chester's bag of candy")!!
        val ctx = ctx(CharacterState(), Preferences(MapSettings()))
        assertEquals(1, maximumUses(item.id, item.name, ctx))
    }

    @Test
    fun restoreItem_capsByNeededHp() {
        val item = ItemDatabase.getByName("aspirin")!!
        val ctx = ctx(
            CharacterState(currentHp = 50, maxHp = 100, currentMp = 0, maxMp = 100),
        )
        assertEquals(1, maximumUses(item.id, item.name, ctx))
    }

    @Test
    fun limitMode_blocksFood() {
        val ctx = ctx(
            CharacterState(fullness = 0, fullnessLimit = 15, limitMode = "spelunky"),
        )
        assertEquals(0, maximumUses(471, "hot wing", ctx))
    }

    @Test
    fun genericUsableItem_returnsMaxValue() {
        val item = ItemDatabase.getByName("ten-leaf clover")!!
        val ctx = ctx(CharacterState())
        assertEquals(Int.MAX_VALUE, maximumUses(item.id, item.name, ctx))
    }

    @Test
    fun multiFight_returnsZero() {
        val ctx = ctx(
            CharacterState(fullness = 0, fullnessLimit = 15),
            inMultiFight = true,
        )
        assertEquals(0, maximumUses(471, "hot wing", ctx))
    }

    @Test
    fun choiceFollowsFight_returnsZero() {
        val ctx = ctx(
            CharacterState(fullness = 0, fullnessLimit = 15),
            choiceFollowsFight = true,
        )
        assertEquals(0, maximumUses(471, "hot wing", ctx))
    }

    @Test
    fun spelunkyLimitItem_blocksOutOfRangeItem() {
        val ctx = ctx(
            CharacterState(limitMode = "spelunky"),
        )
        assertEquals(0, maximumUses(471, "hot wing", ctx))
    }

    @Test
    fun beecore_bannedItem_returnsZero() {
        val item = ItemDatabase.getByName("baseball")!!
        val ctx = ctx(
            CharacterState(challengePath = "Bees Hate You"),
        )
        assertEquals(0, maximumUses(item.id, item.name, ctx))
    }

    @Test
    fun beecore_exceptionItem_returnsMaxValue() {
        val item = ItemDatabase.getByName("ice baby")!!
        val ctx = ctx(
            CharacterState(challengePath = "Bees Hate You"),
        )
        assertEquals(Int.MAX_VALUE, maximumUses(item.id, item.name, ctx))
    }

    @Test
    fun darkChocolateHeart_fullHealth_returnsZero() {
        val heart = ItemDatabase.getByName("heart of dark chocolate")!!
        val prefs = Preferences(MapSettings())
        val full = ctx(CharacterState(currentHp = 100, maxHp = 100), prefs)
        assertEquals(0, maximumUses(heart.id, heart.name, full))
        val hurt = ctx(CharacterState(currentHp = 10, maxHp = 100), prefs)
        assertEquals(1, maximumUses(heart.id, heart.name, hurt))
        prefs.setBoolean("_darkChocolateHeart", true)
        assertEquals(0, maximumUses(heart.id, heart.name, hurt))
    }

    @Test
    fun resolution_dailyAdvCap_returnsZero() {
        val item = ItemDatabase.getByName("resolution: be more adventurous")!!
        val prefs = Preferences(MapSettings())
        prefs.setInt("_resolutionAdv", 9)
        assertEquals(Int.MAX_VALUE, maximumUses(item.id, item.name, ctx(CharacterState(), prefs)))
        prefs.setInt("_resolutionAdv", 10)
        assertEquals(0, maximumUses(item.id, item.name, ctx(CharacterState(), prefs)))
    }

    @Test
    fun fireStartingKit_unbrokenHippyStone_returnsZero() {
        val kit = ItemDatabase.getByName("CSA fire-starting kit")!!
        val prefs = Preferences(MapSettings())
        prefs.setInt("choiceAdventure595", 1)
        val blocked = ctx(CharacterState(hippyStoneBroken = false), prefs)
        assertEquals(0, maximumUses(kit.id, kit.name, blocked))
        val open = ctx(CharacterState(hippyStoneBroken = true), prefs)
        assertEquals(1, maximumUses(kit.id, kit.name, open))
    }

    @Test
    fun leftBearArm_countsRightBearArms() {
        val left = ItemDatabase.getByName("left bear arm")!!
        val none = ctx(CharacterState(), accessibleCount = { 0 })
        assertEquals(0, maximumUses(left.id, left.name, none))
        val three = ctx(CharacterState(), accessibleCount = { id -> if (id == 5791) 3 else 0 })
        assertEquals(3, maximumUses(left.id, left.name, three))
    }

    @Test
    fun sushiRollingMat_oneUseUntilInstalled() {
        val mat = ItemDatabase.getByName("sushi-rolling mat")!!
        val prefs = Preferences(MapSettings())
        assertEquals(1, maximumUses(mat.id, mat.name, ctx(CharacterState(), prefs)))
        prefs.setBoolean("hasSushiMat", true)
        assertEquals(0, maximumUses(mat.id, mat.name, ctx(CharacterState(), prefs)))
    }

    @Test
    fun eternalCarBattery_fullMp_returnsZero() {
        val battery = ItemDatabase.getByName("eternal car battery")!!
        val prefs = Preferences(MapSettings())
        val full = ctx(CharacterState(currentMp = 100, maxMp = 100), prefs)
        assertEquals(0, maximumUses(battery.id, battery.name, full))
        val drained = ctx(CharacterState(currentMp = 0, maxMp = 100), prefs)
        assertEquals(1, maximumUses(battery.id, battery.name, drained))
        prefs.setBoolean("_eternalCarBatteryUsed", true)
        assertEquals(0, maximumUses(battery.id, battery.name, drained))
    }

    @Test
    fun folder_openSlot_returnsOne() {
        val folder = ItemDatabase.getByName("folder (red)")!!
        val empty = ctx(CharacterState())
        assertEquals(1, maximumUses(folder.id, folder.name, empty))
        val aftercoreFull = ctx(
            CharacterState(
                equipment = mapOf(
                    EquipmentSlot.FOLDER1 to "folder (blue)",
                    EquipmentSlot.FOLDER2 to "folder (green)",
                    EquipmentSlot.FOLDER3 to "folder (white)",
                ),
            ),
        )
        assertEquals(0, maximumUses(folder.id, folder.name, aftercoreFull))
        val highSchool = ctx(
            CharacterState(
                challengePath = AscensionPath.KOLHS.apiName,
                equipment = mapOf(
                    EquipmentSlot.FOLDER1 to "folder (blue)",
                    EquipmentSlot.FOLDER2 to "folder (green)",
                    EquipmentSlot.FOLDER3 to "folder (white)",
                ),
            ),
        )
        assertEquals(1, maximumUses(6640, "folder (Stinky Trash Kid)", highSchool))
    }

    @Test
    fun pastaAdditive_requiresPastamancerAndDailyFlag() {
        val pasta = ItemDatabase.getByName("experimental carbon fiber pasta additive")!!
        val prefs = Preferences(MapSettings())
        val other = ctx(CharacterState(characterClass = CharacterClass.SEAL_CLUBBER.id), prefs)
        assertEquals(0, maximumUses(pasta.id, pasta.name, other))
        val chef = ctx(CharacterState(characterClass = CharacterClass.PASTAMANCER.id), prefs)
        assertEquals(1, maximumUses(pasta.id, pasta.name, chef))
        prefs.setBoolean("_pastaAdditive", true)
        assertEquals(0, maximumUses(pasta.id, pasta.name, chef))
    }

    @Test
    fun chronerCross_requiresAChroner() {
        val cross = ItemDatabase.getByName("Chroner cross")!!
        val prefs = Preferences(MapSettings())
        val none = ctx(CharacterState(), prefs, accessibleCount = { 0 })
        assertEquals(0, maximumUses(cross.id, cross.name, none))
        val holding = ctx(CharacterState(), prefs, accessibleCount = { id -> if (id == 7567) 1 else 0 })
        assertEquals(1, maximumUses(cross.id, cross.name, holding))
        prefs.setBoolean("_chronerCrossUsed", true)
        assertEquals(0, maximumUses(cross.id, cross.name, holding))
    }

    @Test
    fun gaudyKey_requiresPirateGear() {
        val key = ItemDatabase.getByName("gaudy key")!!
        assertEquals(0, maximumUses(key.id, key.name, ctx(CharacterState())))
        val fledges = ctx(
            CharacterState(equipment = mapOf(EquipmentSlot.ACC1 to "pirate fledges")),
        )
        assertEquals(Int.MAX_VALUE, maximumUses(key.id, key.name, fledges))
        val pieces = OutfitDatabase.getById(9)!!.equipment
        val outfit = pieces.mapIndexed { index, name ->
            EquipmentSlot.entries[index] to name
        }.toMap()
        assertEquals(Int.MAX_VALUE, maximumUses(key.id, key.name, ctx(CharacterState(equipment = outfit))))
    }

    @Test
    fun bittycar_blocksTheActiveModel() {
        val hot = ItemDatabase.getByName("BittyCar HotCar")!!
        val meat = ItemDatabase.getByName("BittyCar MeatCar")!!
        val prefs = Preferences(MapSettings())
        assertEquals(1, maximumUses(hot.id, hot.name, ctx(CharacterState(), prefs)))
        prefs.setString("_bittycar", "hotcar")
        assertEquals(0, maximumUses(hot.id, hot.name, ctx(CharacterState(), prefs)))
        assertEquals(1, maximumUses(meat.id, meat.name, ctx(CharacterState(), prefs)))
    }

    @Test
    fun stillBeatingSpleen_oncePerAscension() {
        val spleen = ItemDatabase.getByName("still-beating spleen")!!
        val prefs = Preferences(MapSettings())
        val fresh = ctx(CharacterState(ascensionNumber = 4), prefs)
        assertEquals(1, maximumUses(spleen.id, spleen.name, fresh))
        prefs.setInt("lastStillBeatingSpleen", 4)
        assertEquals(0, maximumUses(spleen.id, spleen.name, fresh))
    }

    @Test
    fun mayo_needsClinicAndEmptyMouth() {
        val mayonex = ItemDatabase.getByName("Mayonex")!!
        val mayoflex = ItemDatabase.getByName("Mayoflex")!!
        val prefs = Preferences(MapSettings())
        assertEquals(0, maximumUses(mayonex.id, mayonex.name, ctx(CharacterState(), prefs)))
        prefs.setInt("_currentWorkshedItemId", 8260)
        assertEquals(1, maximumUses(mayonex.id, mayonex.name, ctx(CharacterState(), prefs)))
        assertEquals(1, maximumUses(mayoflex.id, mayoflex.name, ctx(CharacterState(), prefs)))
        prefs.setString("mayoInMouth", "Mayonex")
        assertEquals(0, maximumUses(mayoflex.id, mayoflex.name, ctx(CharacterState(), prefs)))
    }

    @Test
    fun holorecord_needsWristBoy() {
        val power = ItemDatabase.getByName("Power-Guy 2000 holo-record")!!
        val drunk = ItemDatabase.getByName("Drunk Uncles holo-record")!!
        val none = ctx(CharacterState(), accessibleCount = { 0 })
        assertEquals(0, maximumUses(power.id, power.name, none))
        val holding = ctx(CharacterState(), accessibleCount = { id -> if (id == 9102) 1 else 0 })
        assertEquals(Int.MAX_VALUE, maximumUses(power.id, power.name, holding))
        assertEquals(Int.MAX_VALUE, maximumUses(drunk.id, drunk.name, holding))
    }

    @Test
    fun classBook_matchesAscensionClass() {
        val slap = ItemDatabase.getByName("The Art of Slapfighting")!!
        val used = ItemDatabase.getByName("Uncle Romulus (used)")!!
        assertEquals("Seal Clubber", itemToClass(slap.id))
        assertEquals("Turtle Tamer", itemToClass(used.id))
        assertEquals(
            Int.MAX_VALUE,
            maximumUses(slap.id, slap.name, ctx(CharacterState(characterClass = CharacterClass.SEAL_CLUBBER.id))),
        )
        assertEquals(
            Int.MAX_VALUE,
            maximumUses(used.id, used.name, ctx(CharacterState(characterClass = CharacterClass.TURTLE_TAMER.id))),
        )
    }

    @Test
    fun classBook_wrongClass_returnsZero() {
        val inigo = ItemDatabase.getByName("Inigo's Incantation of Inspiration")!!
        assertEquals(
            0,
            maximumUses(inigo.id, inigo.name, ctx(CharacterState(characterClass = CharacterClass.SEAL_CLUBBER.id))),
        )
        assertEquals(
            0,
            maximumUses(4406, "The Art of Slapfighting", ctx(CharacterState(characterClass = CharacterClass.TURTLE_TAMER.id))),
        )
    }

    @Test
    fun cobsKnobMap_returnsEncryptionKeyCount() {
        val map = ItemDatabase.getByName("Cobb's Knob map")!!
        val ctx = ctx(
            CharacterState(),
            accessibleCount = { id ->
                if (id == ItemDatabase.ENCRYPTION_KEY) 3 else 0
            },
        )
        assertEquals(3, maximumUses(map.id, map.name, ctx))
    }

    @Test
    fun dailyLimitDatabase_getUsesRemaining() {
        val item = ItemDatabase.getByName("chester's bag of candy")!!
        val entry = DailyLimitDatabase.getEntry(item.id, DailyLimitKind.USE)!!
        val prefs = Preferences(MapSettings())
        prefs.setInt("_bagOfCandyUsed", 1)
        assertEquals(0, DailyLimitDatabase.getUsesRemaining(entry, prefs))
    }

    private fun ctx(
        character: CharacterState,
        preferences: Preferences? = null,
        inMultiFight: Boolean = false,
        choiceFollowsFight: Boolean = false,
        accessibleCount: (Int) -> Int = { 0 },
    ) = ItemUseLimitsContext(
        character = character,
        preferences = preferences,
        expressionContext = ExpressionContext(
            characterMaxHp = character.maxHp,
            characterMaxMp = character.maxMp,
            characterCurrentHp = character.currentHp,
            challengePath = character.challengePath,
        ),
        inMultiFight = inMultiFight,
        choiceFollowsFight = choiceFollowsFight,
        accessibleCount = accessibleCount,
    )
}
