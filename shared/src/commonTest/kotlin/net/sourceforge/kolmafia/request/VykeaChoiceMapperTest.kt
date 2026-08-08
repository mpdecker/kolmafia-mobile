package net.sourceforge.kolmafia.request

import kotlin.test.Test
import kotlin.test.assertEquals

class VykeaChoiceMapperTest {

    @Test
    fun choice1120_mapsPlankAndRail() {
        assertEquals(1, VykeaChoiceMapper.optionFor(1120, VykeaChoiceMapper.PLANK_ID, 5))
        assertEquals(2, VykeaChoiceMapper.optionFor(1120, VykeaChoiceMapper.RAIL_ID, 5))
        assertEquals(0, VykeaChoiceMapper.optionFor(1120, VykeaChoiceMapper.BRACKET_ID, 5))
    }

    @Test
    fun choice1121_mapsRunesOrSkips() {
        assertEquals(1, VykeaChoiceMapper.optionFor(1121, VykeaChoiceMapper.FRENZY_RUNE_ID, 1))
        assertEquals(2, VykeaChoiceMapper.optionFor(1121, VykeaChoiceMapper.BLOOD_RUNE_ID, 1))
        assertEquals(3, VykeaChoiceMapper.optionFor(1121, VykeaChoiceMapper.LIGHTNING_RUNE_ID, 1))
        assertEquals(VykeaChoiceMapper.SKIP_OPTION, VykeaChoiceMapper.optionFor(1121, VykeaChoiceMapper.PLANK_ID, 5))
    }

    @Test
    fun choice1122_mapsDowelCountsOrSkips() {
        assertEquals(1, VykeaChoiceMapper.optionFor(1122, VykeaChoiceMapper.DOWEL_ID, 1))
        assertEquals(2, VykeaChoiceMapper.optionFor(1122, VykeaChoiceMapper.DOWEL_ID, 11))
        assertEquals(3, VykeaChoiceMapper.optionFor(1122, VykeaChoiceMapper.DOWEL_ID, 23))
        assertEquals(4, VykeaChoiceMapper.optionFor(1122, VykeaChoiceMapper.DOWEL_ID, 37))
        assertEquals(0, VykeaChoiceMapper.optionFor(1122, VykeaChoiceMapper.DOWEL_ID, 5))
        assertEquals(VykeaChoiceMapper.SKIP_OPTION, VykeaChoiceMapper.optionFor(1122, VykeaChoiceMapper.PLANK_ID, 5))
    }

    @Test
    fun choice1123_mapsComponents() {
        assertEquals(1, VykeaChoiceMapper.optionFor(1123, VykeaChoiceMapper.PLANK_ID, 5))
        assertEquals(2, VykeaChoiceMapper.optionFor(1123, VykeaChoiceMapper.RAIL_ID, 5))
        assertEquals(3, VykeaChoiceMapper.optionFor(1123, VykeaChoiceMapper.BRACKET_ID, 5))
    }
}
