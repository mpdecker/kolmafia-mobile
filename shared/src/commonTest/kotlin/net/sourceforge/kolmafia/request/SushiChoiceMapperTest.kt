package net.sourceforge.kolmafia.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse

class SushiChoiceMapperTest {

    @Test
    fun formFields_beefyNigiri() {
        val fields = SushiChoiceMapper.formFields("beefy nigiri")
        assertEquals("Yep.", fields?.get("action"))
        assertEquals("1", fields?.get("whichsushi"))
        assertTrue(fields?.containsKey("whichtopping") == false)
    }

    @Test
    fun formFields_magicalBeefyMaki() {
        registerMapperItems()
        val fields = SushiChoiceMapper.formFields("magical beefy maki")
        assertEquals("4", fields?.get("whichsushi"))
        assertEquals(CAVIAR_ID.toString(), fields?.get("whichtopping"))
    }

    @Test
    fun formFields_giantDragonRoll() {
        registerMapperItems()
        val fields = SushiChoiceMapper.formFields("giant dragon roll")
        assertEquals("4", fields?.get("whichsushi"))
        assertEquals(CUCUMBER_ID.toString(), fields?.get("whichfilling1"))
    }

    @Test
    fun formFields_tempuraAvocadoBentoWithEelSauce() {
        registerMapperItems()
        val fields = SushiChoiceMapper.formFields("tempura avocado bento box with eel sauce")
        assertEquals("7", fields?.get("whichsushi"))
        assertEquals(TEMPURA_AVOCADO_ID.toString(), fields?.get("veggie"))
        assertEquals(EEL_SAUCE_ID.toString(), fields?.get("dipping"))
    }

    @Test
    fun formFields_yuletideWiseDragonRoll() {
        registerMapperItems()
        val fields = SushiChoiceMapper.formFields("Yuletide wise dragon roll")
        assertEquals("5", fields?.get("whichsushi"))
        assertEquals(CUCUMBER_ID.toString(), fields?.get("whichfilling1"))
        assertEquals(PEPPERMINT_EEL_SAUCE_ID.toString(), fields?.get("whichtopping"))
    }

    @Test
    fun formFields_unknownName_returnsNull() {
        assertNull(SushiChoiceMapper.formFields("not sushi"))
    }

    @Test
    fun resultNameFromFormFields_beefyNigiri_roundTrip() {
        val fields = SushiChoiceMapper.formFields("beefy nigiri")
        assertEquals("beefy nigiri", SushiChoiceMapper.resultNameFromFormFields(fields!!))
    }

    @Test
    fun resultNameFromFormFields_giantDragonRoll_roundTrip() {
        registerMapperItems()
        val fields = SushiChoiceMapper.formFields("giant dragon roll")
        assertEquals("giant dragon roll", SushiChoiceMapper.resultNameFromFormFields(fields!!))
    }

    @Test
    fun resultNameFromFormFields_tempuraAvocadoBentoWithEelSauce_roundTrip() {
        registerMapperItems()
        val fields = SushiChoiceMapper.formFields("tempura avocado bento box with eel sauce")
        assertEquals(
            "tempura avocado bento box with eel sauce",
            SushiChoiceMapper.resultNameFromFormFields(fields!!),
        )
    }

    @Test
    fun formFieldsFromUrl_beefyNigiri() {
        val url = "sushi.php?action=Yep.&whichsushi=1"
        val fields = SushiChoiceMapper.formFieldsFromUrl(url)
        assertEquals("beefy nigiri", SushiChoiceMapper.resultNameFromFormFields(fields!!))
    }

    @Test
    fun formFieldsFromUrl_magicalBeefyMaki() {
        registerMapperItems()
        val url = "https://www.kingdomofloathing.com/sushi.php?action=Yep.&whichsushi=4&whichtopping=$CAVIAR_ID"
        val fields = SushiChoiceMapper.formFieldsFromUrl(url)
        assertEquals("magical beefy maki", SushiChoiceMapper.resultNameFromFormFields(fields!!))
    }

    @Test
    fun formFieldsFromUrl_giantDragonRoll() {
        registerMapperItems()
        val url = "sushi.php?whichsushi=4&whichfilling1=$CUCUMBER_ID"
        val fields = SushiChoiceMapper.formFieldsFromUrl(url)
        assertEquals("giant dragon roll", SushiChoiceMapper.resultNameFromFormFields(fields!!))
    }

    @Test
    fun formFieldsFromUrl_tempuraAvocadoBentoWithEelSauce() {
        registerMapperItems()
        val url = "sushi.php?whichsushi=7&veggie=$TEMPURA_AVOCADO_ID&dipping=$EEL_SAUCE_ID"
        val fields = SushiChoiceMapper.formFieldsFromUrl(url)
        assertEquals(
            "tempura avocado bento box with eel sauce",
            SushiChoiceMapper.resultNameFromFormFields(fields!!),
        )
    }

    @Test
    fun formFieldsFromUrl_withoutWhichsushi_returnsNull() {
        assertNull(SushiChoiceMapper.formFieldsFromUrl("sushi.php"))
    }

    private fun registerMapperItems() {
        listOf(
            CAVIAR_ID to "dragonfish caviar",
            CUCUMBER_ID to "sea cucumber",
            TEMPURA_AVOCADO_ID to "tempura avocado",
            EEL_SAUCE_ID to "eel sauce",
            PEPPERMINT_EEL_SAUCE_ID to "peppermint eel sauce",
        ).forEach { (id, name) ->
            ItemDatabase.registerForTest(
                ItemData(
                    id = id,
                    name = name,
                    descId = "d$id",
                    image = "img",
                    primaryUse = ItemPrimaryUse.NONE,
                    secondaryUses = emptySet(),
                    access = setOf('t', 'd'),
                    autosellPrice = 100,
                    plural = null,
                ),
            )
        }
    }

    companion object {
        private const val CAVIAR_ID = 89001
        private const val CUCUMBER_ID = 89002
        private const val TEMPURA_AVOCADO_ID = 89003
        private const val EEL_SAUCE_ID = 89004
        private const val PEPPERMINT_EEL_SAUCE_ID = 89005
    }
}
