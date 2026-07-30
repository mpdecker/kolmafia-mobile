package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.ash.currentDateString
import net.sourceforge.kolmafia.platform.UserDataFileAppender
import net.sourceforge.kolmafia.preferences.Preferences

/** Tab-delimited Bastille spading logs (desktop BastilleBattalionManager battle/cheese files). */
object BastilleBattalionFileLog {

    private const val BATTLE_FILE = "Bastille.battles.txt"
    private const val CHEESE_FILE = "Bastille.cheese.txt"
    private const val PREF_LOG = "logBastilleBattalionBattles"
    private const val PREF_GAMES = "_bastilleGames"

    fun saveBattle(battle: BastilleBattle, prefs: Preferences, playerId: Int) {
        if (!prefs.getBoolean(PREF_LOG)) return
        val game = prefs.getInt(PREF_GAMES) + 1
        val key = generateKey(playerId, game, battle.number)
        val results = battle.results ?: return
        val line = joinFields(
            "\t",
            key,
            battle.number.toString(),
            battle.stats.ma.toString(),
            battle.stats.md.toString(),
            battle.stats.ca.toString(),
            battle.stats.cd.toString(),
            battle.stats.pa.toString(),
            battle.stats.pd.toString(),
            battle.boosts.toString(),
            battle.enemy?.prefix ?: "",
            battle.stance.toString(),
            results.aggressor.toString(),
            results.military.toString(),
            results.castle.toString(),
            results.psychological.toString(),
            battle.cheese.toString(),
        )
        UserDataFileAppender.appendLine(BATTLE_FILE, line)
    }

    fun saveCheese(cheese: BastilleCheeseRecord, prefs: Preferences, playerId: Int) {
        if (!prefs.getBoolean(PREF_LOG)) return
        val game = prefs.getInt(PREF_GAMES) + 1
        val key = generateKey(playerId, game, cheese.turn)
        val line = joinFields(
            "\t",
            key,
            cheese.turn.toString(),
            cheese.encounter.name,
            cheese.stat.name,
            cheese.statBonus.toString(),
            cheese.potion.toString(),
            cheese.cheese.toString(),
        )
        UserDataFileAppender.appendLine(CHEESE_FILE, line)
    }

    private fun generateKey(playerId: Int, game: Int, round: Int): String =
        joinFields(".", currentDateString(), playerId.toString(), game.toString(), round.toString())

    private fun joinFields(separator: String, vararg fields: String): String =
        fields.joinToString(separator)
}
