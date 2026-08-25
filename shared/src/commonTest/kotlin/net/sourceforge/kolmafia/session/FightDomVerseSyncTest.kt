package net.sourceforge.kolmafia.session

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.data.MonsterDefinition

class FightCombatModeSyncTest {

    @BeforeTest
    fun setUp() {
        FightCombatModeSync.reset()
        ChoiceCombatAshState.reset()
    }

    @Test
    fun detectsHaikuDungeon() {
        FightCombatModeSync.detectModes(adventureId = "138")
        assertTrue(FightCombatModeSync.haiku)
        assertTrue(FightCombatModeSync.isGarbled)
    }

    @Test
    fun detectsAnapestSuburbs() {
        FightCombatModeSync.detectModes(adventureId = "277")
        assertTrue(FightCombatModeSync.anapest)
    }

    @Test
    fun detectsMachineElf() {
        FightCombatModeSync.detectModes(adventureId = "458")
        assertTrue(FightCombatModeSync.machineElf)
    }

    @Test
    fun synchronizesOnturn() {
        ChoiceCombatAshState.currentRound = 1
        val round = FightCombatModeSync.synchronizeRoundNumber("var onturn = 3;")
        assertEquals(3, round)
        assertEquals(3, ChoiceCombatAshState.currentRound)
    }

    @Test
    fun wonInitiativeJump() {
        assertTrue(FightCombatModeSync.wonInitiative("You get the jump on your opponent."))
        assertFalse(FightCombatModeSync.wonInitiative("Your foe gets the jump."))
    }

    @Test
    fun wonInitiativeThisFightRoundOne() {
        ChoiceCombatAshState.currentRound = 1
        FightCombatModeSync.applyFightHtml(
            html = "You get the jump on your opponent. onturn = 1;",
            isFightStart = true,
        )
        assertTrue(FightCombatModeSync.wonInitiativeThisFight())
    }
}

class FightDamageParserTest {

    @BeforeTest
    fun setUp() {
        MonsterStatusTracker.resetLastMonster()
        MonsterStatusTracker.setNextMonster(testMonster(hp = 200), emptyList())
    }

    @Test
    fun parsesYouDealDamage() {
        val dmg = FightDamageParser.parseNormalDamage("You deal 42 damage to your foe.")
        assertEquals(42, dmg)
    }

    @Test
    fun appliesDamageToTracker() {
        FightDamageParser.apply(
            "<p>You hit for 30 damage.</p><p>You deal 20 damage to your opponent.</p>",
        )
        assertEquals(150, MonsterStatusTracker.getMonsterHealth())
    }

    @Test
    fun ignoresYouLose() {
        assertEquals(0, FightDamageParser.parseNormalDamage("You lose 15 hit points"))
    }

    @Test
    fun healsMonster() {
        MonsterStatusTracker.damageMonster(50)
        FightDamageParser.applyHealFromHtml("The monster regains 20 hit points")
        assertEquals(170, MonsterStatusTracker.getMonsterHealth())
    }

    @Test
    fun delevelsAttackDefense() {
        FightDamageParser.applyDelevelFromHtml(
            """<img src="nicesword.gif"> Attack Power drops by 10
               <img src="whiteshield.gif"> Defense drops by 5""",
        )
        assertEquals(90, MonsterStatusTracker.getMonsterAttack())
        assertEquals(75, MonsterStatusTracker.getMonsterDefense())
    }
}

class FightVerseSyncTest {

    @BeforeTest
    fun setUp() {
        FightCombatModeSync.reset()
        MonsterStatusTracker.resetLastMonster()
        MonsterStatusTracker.setNextMonster(testMonster(hp = 100), emptyList())
    }

    @Test
    fun parseVerseDamageTitle() {
        val html = """<img src="fire.gif" title="Damage: 12">"""
        assertEquals(12, FightVerseSync.parseVerseDamage(html))
    }

    @Test
    fun skipsVerseDamageInMachineElf() {
        FightCombatModeSync.machineElf = true
        val html = """<img src="fire.gif" title="Damage: 12">"""
        assertEquals(0, FightVerseSync.parseVerseDamage(html, machineElf = true))
    }

    @Test
    fun appliesVerseDamageToTracker() {
        FightCombatModeSync.haiku = true
        FightVerseSync.applyVerseDamage("""<img title="Damage: 25">""")
        assertEquals(75, MonsterStatusTracker.getMonsterHealth())
    }
}

class FightCommentSyncTest {

    @BeforeTest
    fun setUp() {
        FightCommentSync.reset()
        MonsterStatusTracker.resetLastMonster()
    }

    @Test
    fun detectsWinLoseComments() {
        FightCommentSync.apply("<!--WINWINWIN-->")
        assertEquals(true, FightCommentSync.lastWon)
        FightCommentSync.apply("<!--LOSELOSELOSE-->")
        assertEquals(false, FightCommentSync.lastWon)
    }

    @Test
    fun parsesMonsterIdComment() {
        FightCommentSync.apply("<!--MONSTERID:1-->")
        assertEquals(1, FightCommentSync.lastMonsterId)
    }
}

class FightDomSyncTest {

    @BeforeTest
    fun setUp() {
        FightDomSync.resetFight()
        ChoiceCombatAshState.reset()
        MonsterStatusTracker.resetLastMonster()
        MonsterStatusTracker.setNextMonster(testMonster(hp = 200), emptyList())
    }

    @Test
    fun orchestratesModesAndDamage() {
        ChoiceCombatAshState.currentRound = 1
        FightDomSync.apply(
            FightDomSync.Context(
                html = """
                    You get the jump on your opponent.
                    var onturn = 1;
                    You deal 40 damage to your foe.
                    <!--WINWINWIN-->
                """.trimIndent(),
                adventureId = "100",
                isFightStart = true,
            ),
        )
        assertTrue(FightCombatModeSync.lastWonInitiative)
        assertEquals(160, MonsterStatusTracker.getMonsterHealth())
        assertEquals(true, FightCommentSync.lastWon)
        assertEquals(1, ChoiceCombatAshState.currentRound)
    }

    @Test
    fun haikuModeFromAdventureId() {
        FightDomSync.apply(
            FightDomSync.Context(
                html = """<img title="Damage: 15"> onturn = 2;""",
                adventureId = "138",
                isFightStart = true,
            ),
        )
        assertTrue(FightCombatModeSync.haiku)
        assertEquals(185, MonsterStatusTracker.getMonsterHealth())
        assertEquals(2, ChoiceCombatAshState.currentRound)
    }
}

private fun testMonster(hp: Int) = MonsterDefinition(
    name = "test monster",
    id = 1,
    image = "test.gif",
    attack = 100,
    defense = 80,
    hp = hp,
    initiative = 50,
    meatDrop = 0,
    phylum = "beast",
    isBoss = false,
    isGhost = false,
    isLucky = false,
    isScaling = false,
    scale = 0,
    cap = 0,
    floor = 0,
    drops = emptyList(),
)
