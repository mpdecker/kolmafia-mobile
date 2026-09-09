package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.TimeTowerSync
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Behavioral Deepen XXXV — quest special-case + Time Tower corpus. */
class QuestSpecialPhase6010Test {

    @Test
    fun telegramAdvanceRules_coversNineQuestlines() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.TELEGRAM, QuestDatabase.STARTED)
        assertEquals(36, QuestSpecialSync.telegramSteps.size)

        assertTrue(QuestAdvanceRules.apply("Find the pastor in his church.", db, prefs))
        assertEquals("step1", db.getProgress(Quest.TELEGRAM))
        assertEquals("Haunted Boneyard", prefs.getString("lttQuestName", ""))
        assertEquals(2, prefs.getInt("lttQuestDifficulty", 0))
    }

    @Test
    fun partyFair_remainingBillIsDjNotMeat() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.PARTY_FAIR, QuestDatabase.STARTED)
        assertTrue(QuestSpecialSync.apply("Remaining bill: 4,200 Meat", db, prefs))
        assertEquals("dj", prefs.getString("_questPartyFairQuest", ""))
        assertEquals("4200", prefs.getString("_questPartyFairProgress", ""))
    }

    @Test
    fun doctorBag_maladyAliasesCaseInsensitive() {
        assertEquals(
            "red blood cells",
            DoctorBagChoiceSync.resolveMaladyItem("Thin Blood Syndrome"),
        )
        assertEquals(
            "palm-frond fan",
            DoctorBagChoiceSync.resolveMaladyItem("TROPICAL HEATSTROKE"),
        )
        val prefs = Preferences(MapSettings())
        assertTrue(
            DoctorBagChoiceSync.applyVisit(
                DoctorBagChoiceSync.CHOICE_ID,
                "We've received a report of a patient Thin Blood Syndrome, in The Haunted Bedroom.",
                prefs,
            ),
        )
        assertEquals("red blood cells", prefs.getString("doctorBagQuestItem", ""))
        assertEquals("The Haunted Bedroom", prefs.getString("doctorBagQuestLocation", ""))
    }

    @Test
    fun pirateRealm_applyFromAdventureHtml_sailAndFinish() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            PirateRealmSync.applyFromAdventureHtml(
                """Welcome to Pirate Realm <img src="sail1.gif">""",
                db,
                prefs,
            ),
        )
        assertEquals("step1", db.getProgress(Quest.PIRATEREALM))
        assertTrue(
            PirateRealmSync.applyFromAdventureHtml(
                "an envelope with your name on it and a piratical blunderbuss",
                db,
                prefs,
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.PIRATEREALM))
        assertTrue(prefs.getBoolean("pirateRealmUnlockedBlunderbuss", false))
    }

    @Test
    fun timeTower_syncFromMainPhpTwitchtower() {
        val prefs = Preferences(MapSettings())
        TimeTowerSync.syncFromMainPhp("""<a href="place.php?whichplace=twitch"><img src="twitchtower.gif"></a>""", prefs)
        assertTrue(prefs.getBoolean(TimeTowerSync.PREF, false))
        TimeTowerSync.syncFromMainPhp("""<html>no tower here</html>""", prefs)
        assertFalse(prefs.getBoolean(TimeTowerSync.PREF, true))
    }

    @Test
    fun questLogSync_pastStartedSignals() {
        assertTrue(QuestLogSync.shouldSync("Remaining bill: 100 Meat"))
        assertTrue(QuestLogSync.shouldSync("""<img src="sail2.gif">"""))
        assertTrue(QuestLogSync.shouldSync("Ask around the Rough Diamond Saloon"))
        assertTrue(QuestLogSync.shouldSync("to the patient in The Haunted Bedroom"))
    }
}
