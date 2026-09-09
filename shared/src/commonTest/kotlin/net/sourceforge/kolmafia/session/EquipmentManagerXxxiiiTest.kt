package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.WeaponType
import net.sourceforge.kolmafia.preferences.Preferences

class EquipmentManagerXxxiiiTest {
    private lateinit var character: KoLCharacter
    private lateinit var manager: EquipmentManager

    @BeforeTest
    fun setUp() = runBlocking {
        ItemDatabase.load()
        EquipmentDatabase.load()
        character = KoLCharacter()
        manager = EquipmentManager(character)
    }

    // ── isDualWielding ───────────────────────────────────────────────────────

    @Test
    fun isDualWielding_aliasesUsingTwoWeapons() {
        assertEquals(manager.usingTwoWeapons(), manager.isDualWielding())
    }

    // ── holsteredSixgun ──────────────────────────────────────────────────────

    @Test
    fun holsteredSixgun_falseWhenEmpty() {
        assertFalse(manager.holsteredSixgun())
    }

    // ── usingShield ──────────────────────────────────────────────────────────

    @Test
    fun usingShield_falseWhenNoOffhand() {
        assertFalse(manager.usingShield())
    }

    @Test
    fun usingShield_trueWhenShieldEquipped() {
        val shield = ItemDatabase.getByName("vinyl shield") ?: return
        character.updateEquipment(EquipmentSlot.OFFHAND, shield.name)
        assertTrue(manager.usingShield())
    }

    // ── usingCanOfBeans ──────────────────────────────────────────────────────

    @Test
    fun usingCanOfBeans_falseWhenEmpty() {
        assertFalse(manager.usingCanOfBeans())
    }

    // ── wieldingClub ─────────────────────────────────────────────────────────

    @Test
    fun wieldingClub_falseWhenNoWeapon() {
        assertFalse(manager.wieldingClub())
    }

    @Test
    fun wieldingClub_trueWithClub() {
        val club = ItemDatabase.getByName("seal-clubbing club") ?: return
        character.updateEquipment(EquipmentSlot.WEAPON, club.name)
        assertTrue(manager.wieldingClub())
    }

    @Test
    fun wieldingClub_swordWithIronPalms() {
        val sword = ItemDatabase.getByName("sweet ninja sword") ?: return
        character.updateEquipment(EquipmentSlot.WEAPON, sword.name)
        assertFalse(manager.wieldingClub(includeEffect = false))
        // With Iron Palms effect
        manager.hasEffect = { it == EquipmentManager.IRON_PALMS_EFFECT }
        assertTrue(manager.wieldingClub(includeEffect = true))
    }

    // ── wieldingKnife ────────────────────────────────────────────────────────

    @Test
    fun wieldingKnife_falseWhenNoWeapon() {
        assertFalse(manager.wieldingKnife())
    }

    // ── wieldingSword ────────────────────────────────────────────────────────

    @Test
    fun wieldingSword_trueWithSword() {
        val sword = ItemDatabase.getByName("sweet ninja sword") ?: return
        character.updateEquipment(EquipmentSlot.WEAPON, sword.name)
        assertTrue(manager.wieldingSword())
    }

    @Test
    fun wieldingSword_falseWithIronPalmsEffect() {
        val sword = ItemDatabase.getByName("sweet ninja sword") ?: return
        character.updateEquipment(EquipmentSlot.WEAPON, sword.name)
        manager.hasEffect = { it == EquipmentManager.IRON_PALMS_EFFECT }
        assertFalse(manager.wieldingSword(includeEffect = true))
    }

    // ── wieldingGun ──────────────────────────────────────────────────────────

    @Test
    fun wieldingGun_falseWhenNoWeapon() {
        assertFalse(manager.wieldingGun())
    }

    // ── getWeaponType ────────────────────────────────────────────────────────

    @Test
    fun getWeaponType_noneWhenEmpty() {
        assertEquals(WeaponType.NONE, manager.getWeaponType())
    }

    // ── getHitStatType ───────────────────────────────────────────────────────

    @Test
    fun getHitStatType_defaultsMuscle() {
        assertEquals("Muscle", manager.getHitStatType())
    }

    // ── getAdjustedHitStat ───────────────────────────────────────────────────

    @Test
    fun getAdjustedHitStat_usesBuffedMusc() {
        character.updateFromApiResponse(CharacterApiResponse(buffedmus = "100", mus = "50"))
        assertEquals(100, manager.getAdjustedHitStat())
    }

    @Test
    fun getAdjustedHitStat_maxValueWhenCantMiss() {
        manager.hasBooleanModifier = { true }
        assertEquals(Int.MAX_VALUE, manager.getAdjustedHitStat())
    }

    // ── powerfulGlove ────────────────────────────────────────────────────────

    @Test
    fun powerfulGloveAvailableBatteryPower_default100() {
        val prefs = Preferences(MapSettings())
        assertEquals(100, manager.powerfulGloveAvailableBatteryPower(prefs))
    }

    @Test
    fun powerfulGloveAvailableBatteryPower_subtractsUsed() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_powerfulGloveBatteryPowerUsed", 37)
        assertEquals(63, manager.powerfulGloveAvailableBatteryPower(prefs))
    }

    @Test
    fun powerfulGloveUsableBatteryPower_zeroWhenNotEquipped() {
        val prefs = Preferences(MapSettings())
        assertEquals(0, manager.powerfulGloveUsableBatteryPower(prefs))
    }

    @Test
    fun powerfulGloveUsableBatteryPower_returnsChargeWhenEquipped() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_powerfulGloveBatteryPowerUsed", 20)
        character.updateEquipment(EquipmentSlot.ACC1, "Powerful Glove")
        assertEquals(80, manager.powerfulGloveUsableBatteryPower(prefs))
    }

    // ── fireExtinguisher ─────────────────────────────────────────────────────

    @Test
    fun fireExtinguisherAvailableFoam_readsChargePref() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("_fireExtinguisherCharge", 42)
        assertEquals(42, manager.fireExtinguisherAvailableFoam(prefs))
    }

    // ── hasOutfit / isWearingOutfit ───────────────────────────────────────────

    @Test
    fun hasOutfit_falseWhenEmpty() {
        assertFalse(manager.hasOutfit(listOf("hat", "pants")))
    }

    @Test
    fun hasOutfit_trueWhenAllEquipped() {
        character.updateEquipment(EquipmentSlot.HAT, "seal-skull helmet")
        character.updateEquipment(EquipmentSlot.WEAPON, "seal-clubbing club")
        character.updateEquipment(EquipmentSlot.PANTS, "old sweatpants")
        assertTrue(manager.hasOutfit(listOf("seal-skull helmet", "seal-clubbing club", "old sweatpants")))
    }

    @Test
    fun isWearingOutfit_synonymForHasOutfit() {
        character.updateEquipment(EquipmentSlot.HAT, "seal-skull helmet")
        assertEquals(
            manager.hasOutfit(listOf("seal-skull helmet")),
            manager.isWearingOutfit(listOf("seal-skull helmet")),
        )
    }

    // ── equippedCount ────────────────────────────────────────────────────────

    @Test
    fun equippedCount_zeroWhenNotEquipped() {
        assertEquals(0, manager.equippedCount(1))
    }

    @Test
    fun equippedCount_countsAllSlots() {
        val pants = ItemDatabase.getByName("old sweatpants") ?: return
        character.updateEquipment(EquipmentSlot.PANTS, pants.name)
        assertEquals(1, manager.equippedCount(pants.id))
    }

    // ── DI callbacks ─────────────────────────────────────────────────────────

    @Test
    fun hasEffect_defaultReturnsFalse() {
        assertFalse(manager.hasEffect("Iron Palms"))
    }

    @Test
    fun hasSkillId_defaultReturnsFalse() {
        assertFalse(manager.hasSkillId(5029))
    }
}
