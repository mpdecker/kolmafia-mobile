package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class GreyYouManagerTest {

    @BeforeTest
    fun setUp() = runTest {
        GreyYouManager.resetForTest()
        MonsterDatabase.load()
        SkillDefinitionDatabase.load()
    }

    @AfterTest
    fun tearDown() {
        GreyYouManager.resetForTest()
    }

    @Test
    fun loadRegistry_includesFullGooSkillCatalog() {
        GreyYouManager.loadRegistry()
        assertTrue(GreyYouManager.allGooSkills.size >= 60)
        assertTrue(GreyYouManager.zoneAbsorptions.isNotEmpty())
    }

    @Test
    fun parseAbsorptions_marksKnownMonster() {
        GreyYouManager.loadRegistry()
        val warwelf = MonsterDatabase.getByName("warwelf")
        requireNotNull(warwelf)
        GreyYouManager.parseMonsterAbsorptions(
            "Absorbed 5 adventures from warwelf.<!-- ${warwelf.id} -->",
        )
        assertTrue(GreyYouManager.haveAbsorbed(warwelf.id))
    }

    @Test
    fun sortedGooSkills_ordersByType() {
        GreyYouManager.loadRegistry()
        val skills = GreyYouManager.sortedGooSkills("type")
        assertTrue(skills.isNotEmpty())
    }

    @Test
    fun modifierOverlay_includesStatAbsorption() {
        GreyYouManager.loadRegistry()
        val bat = MonsterDatabase.getByName("vampire bat")
        requireNotNull(bat)
        GreyYouManager.absorbedMonsters += bat.id
        val overlay = GreyYouManager.modifierOverlay()
        assertTrue(overlay.contains("Maximum HP"))
    }
}
