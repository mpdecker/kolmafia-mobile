package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.MainStat
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExoticBoostCliV4SupportTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @Test
    fun styx_findBuffId_aliases() {
        assertEquals(446, HeyDezeRequest.findBuffId("muscle"))
        assertEquals(446, HeyDezeRequest.findBuffId("mus"))
        assertEquals(447, HeyDezeRequest.findBuffId("mysticality"))
        assertEquals(447, HeyDezeRequest.findBuffId("myst"))
        assertEquals(448, HeyDezeRequest.findBuffId("moxie"))
        assertEquals(448, HeyDezeRequest.findBuffId("mox"))
        assertEquals(0, HeyDezeRequest.findBuffId("hp"))
    }

    @Test
    fun skeleton_findSkeleton_types() {
        assertEquals(1, SkeletonRequest.findSkeleton("warrior"))
        assertEquals(2, SkeletonRequest.findSkeleton("cleric"))
        assertEquals(3, SkeletonRequest.findSkeleton("wizard"))
        assertEquals(4, SkeletonRequest.findSkeleton("rogue"))
        assertEquals(5, SkeletonRequest.findSkeleton("buddy"))
        assertEquals(0, SkeletonRequest.findSkeleton("lich"))
    }

    @Test
    fun play_resolve_buff_stat_random() {
        assertNull(DeckOfEveryCardRequest.resolvePlay("random").getOrNull())
        assertEquals(
            DeckOfEveryCardRequest.STRENGTH,
            DeckOfEveryCardRequest.resolvePlay("buff mus").getOrNull(),
        )
        assertEquals(
            DeckOfEveryCardRequest.MAGICIAN,
            DeckOfEveryCardRequest.resolvePlay("buff Magicianship").getOrNull(),
        )
        assertEquals(
            DeckOfEveryCardRequest.WHEEL,
            DeckOfEveryCardRequest.resolvePlay("buff items").getOrNull(),
        )
        assertEquals(
            DeckOfEveryCardRequest.RACE,
            DeckOfEveryCardRequest.resolvePlay("buff initiative").getOrNull(),
        )
        assertEquals(
            DeckOfEveryCardRequest.WORLD,
            DeckOfEveryCardRequest.resolvePlay("stat mus").getOrNull(),
        )
        assertEquals(
            DeckOfEveryCardRequest.EMPRESS,
            DeckOfEveryCardRequest.resolvePlay("stat main", MainStat.MYSTICALITY).getOrNull(),
        )
        assertEquals(
            DeckOfEveryCardRequest.FOOL,
            DeckOfEveryCardRequest.resolvePlay("0 - The Fool").getOrNull(),
        )
        assertTrue(DeckOfEveryCardRequest.resolvePlay("phylum beast").isFailure)
        assertTrue(DeckOfEveryCardRequest.resolvePlay("").isFailure)
    }

    @Test
    fun play_selectDeck_and_draw_gate() {
        assertEquals(
            DeckOfEveryCardRequest.DECK_ID,
            DeckOfEveryCardRequest.selectDeck({ if (it == 8382) 1 else 0 }, false),
        )
        assertEquals(
            DeckOfEveryCardRequest.REPLICA_DECK_ID,
            DeckOfEveryCardRequest.selectDeck({ if (it == 11230) 1 else 0 }, true),
        )
        assertNull(DeckOfEveryCardRequest.selectDeck({ 0 }, true))
    }

    @Test
    fun gong_setPath_writes_choiceAdventure_bits() {
        val p = prefs()
        GongRequest.setPath(1, p) // bird
        assertEquals(1, p.getInt("gongPath", 0))
        assertEquals("3", p.getString("choiceAdventure276", ""))
        assertEquals("1", p.getString("choiceAdventure277", ""))
    }

    @Test
    fun gong_parse_bird_mole_roach_set() {
        val bird = GongRequest.parseParameters("bird").getOrNull()
        assertNotNull(bird)
        assertEquals(1, bird.path)
        assertFalse(bird.setOnly)

        val setMole = GongRequest.parseParameters("set mole").getOrNull()
        assertNotNull(setMole)
        assertEquals(2, setMole.path)
        assertTrue(setMole.setOnly)

        val roach = GongRequest.parseParameters("roach mus mus mus").getOrNull()
        assertNotNull(roach)
        assertTrue(roach.path in 4 until GongRequest.GONG_PATHS.size)

        assertTrue(GongRequest.parseParameters("buy bird").isFailure)
        assertTrue(GongRequest.parseParameters("").isFailure)
    }

    @Test
    fun gong_preflight_blocks_active_form() {
        assertNull(
            GongRequest.preflightBlocked(
                charState = net.sourceforge.kolmafia.character.CharacterState(limitMode = ""),
                activeEffects = emptyList(),
            ),
        )
        assertEquals(
            "You can't use a gong right now.",
            GongRequest.preflightBlocked(
                charState = net.sourceforge.kolmafia.character.CharacterState(limitMode = "bird"),
                activeEffects = emptyList(),
            ),
        )
        assertEquals(
            "You can't use a gong right now.",
            GongRequest.preflightBlocked(
                charState = net.sourceforge.kolmafia.character.CharacterState(limitMode = "roach"),
                activeEffects = listOf(EffectData(GongRequest.FORM_OF_ROACH, "Form of...Roach!", 10)),
            ),
        )
        assertNull(
            GongRequest.preflightBlocked(
                charState = net.sourceforge.kolmafia.character.CharacterState(limitMode = "roach"),
                activeEffects = emptyList(),
            ),
        )
    }
}
