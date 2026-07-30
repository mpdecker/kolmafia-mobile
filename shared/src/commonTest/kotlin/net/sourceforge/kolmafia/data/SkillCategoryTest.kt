package net.sourceforge.kolmafia.data

import kotlin.test.Test
import kotlin.test.assertEquals

class SkillCategoryTest {

    @Test
    fun bySkillId_sealClubberRange() {
        assertEquals(SkillCategory.SEAL_CLUBBER, SkillCategory.bySkillId(1003))
    }

    @Test
    fun bySkillId_uncategorizedLowIds() {
        assertEquals(SkillCategory.UNCATEGORIZED, SkillCategory.bySkillId(15))
    }

    @Test
    fun bySkillId_mrSkillsOverride() {
        assertEquals(SkillCategory.MR_SKILLS, SkillCategory.bySkillId(7219))
    }

    @Test
    fun bySkillId_gnomeSkillsOverride() {
        assertEquals(SkillCategory.GNOME_SKILLS, SkillCategory.bySkillId(10))
    }

    @Test
    fun bySkillId_badMoonOverride() {
        assertEquals(SkillCategory.BAD_MOON, SkillCategory.bySkillId(21))
    }

    @Test
    fun bySkillId_sneakyPeteOverride() {
        assertEquals(SkillCategory.AVATAR_OF_SNEAKY_PETE, SkillCategory.bySkillId(7201))
    }
}
