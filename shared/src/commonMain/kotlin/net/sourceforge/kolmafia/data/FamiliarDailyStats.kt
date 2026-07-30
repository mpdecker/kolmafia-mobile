package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop FamiliarData.DropInfo / FightInfo daily drop and fight counters. */
object FamiliarDailyStats {

    data class DropInfo(
        val familiarId: Int,
        val dropItemId: Int,
        val dropName: String,
        val dropTracker: String,
        val dailyCap: Int,
    )

    data class FightInfo(
        val familiarId: Int,
        val fightTracker: String,
        val dailyCap: Int,
    )

    private val dropFamiliars = listOf(
        DropInfo(70, 2655, "absinthe", "_absintheDrops", 5),
        DropInfo(111, 543, "agua", "_aguaDrops", 5),
        DropInfo(53, 1687, "astral", "_astralDrops", 5),
        DropInfo(155, 312, "folio", "_kloopDrops", 5),
        DropInfo(90, 354, "gong", "_gongDrops", 5),
        DropInfo(154, 3862, "grease", "_grooseDrops", 5),
        DropInfo(135, 423, "token", "_tokenDrops", 5),
        DropInfo(148, 1150, "transponder", "_transponderDrops", 5),
        DropInfo(166, 3486, "dream jar", "_dreamJarDrops", 5),
        DropInfo(165, 3169, "psycho jar", "_jungDrops", 1),
        DropInfo(179, 3481, "fairy tale", "_grimFairyTaleDrops", 5),
        DropInfo(178, 3480, "grim mask", "_grimstoneMaskDrops", 1),
        DropInfo(183, 3498, "hot ashes", "_hotAshesDrops", 5),
        DropInfo(188, -1, "turkey booze", "_turkeyBooze", 5),
        DropInfo(192, 3614, "powdered gold", "_powderedGoldDrops", 5),
        DropInfo(193, 3616, "tales", "_spelunkingTalesDrops", 1),
        DropInfo(91, -1, "cotton candy", "_carnieCandyDrops", 10),
        DropInfo(150, -1, "pastes", "_bootStomps", 7),
        DropInfo(180, 3627, "mini-martini", "_miniMartiniDrops", 6),
        DropInfo(196, 3629, "power pill", "_powerPillDrops", 11),
        DropInfo(197, 3629, "power pill", "_powerPillDrops", 11),
        DropInfo(199, 3631, "snowglobe", "_snowglobeDrops", 1),
        DropInfo(201, 3640, "robin's egg", "_robinEggDrops", -1),
        DropInfo(210, 3648, "wax glob", "_waxGlobDrops", -1),
        DropInfo(214, -1, "burning item", "_garbageFireDrops", -1),
        DropInfo(288, -1, "cookbookbat recipe", "_cookbookbatRecipeDrops", 1),
        DropInfo(290, 4518, "grubby wool", "_grubbyWoolDrops", -1),
        DropInfo(294, 4523, "maps", "_mapToACandyRichBlockDrops", -1),
        DropInfo(300, 4531, "mini kiwis", "_miniKiwiDrops", -1),
        DropInfo(326, 4561, "knucklebones", "_knuckleboneDrops", 100),
    )

    private val fightFamiliars = listOf(
        FightInfo(136, "_hipsterAdv", 7),
        FightInfo(160, "_hipsterAdv", 7),
        FightInfo(199, "_machineTunnelsAdv", 5),
    )

    fun getDropInfo(familiarId: Int): DropInfo? =
        dropFamiliars.firstOrNull { it.familiarId == familiarId }

    fun getFightInfo(familiarId: Int): FightInfo? =
        fightFamiliars.firstOrNull { it.familiarId == familiarId }

    fun dropsToday(familiarId: Int, preferences: Preferences?): Int {
        val info = getDropInfo(familiarId) ?: return 0
        return trackerValue(info.dropTracker, preferences)
    }

    fun dropDailyCap(familiarId: Int, preferences: Preferences?): Int {
        val info = getDropInfo(familiarId) ?: return 0
        if (familiarId == 196 || familiarId == 197) {
            val days = preferences?.getInt(Preferences.LAST_DAYCOUNT, 0) ?: 0
            return minOf(1 + days, 11)
        }
        return info.dailyCap
    }

    fun fightsToday(familiarId: Int, preferences: Preferences?): Int {
        val info = getFightInfo(familiarId) ?: return 0
        return trackerValue(info.fightTracker, preferences)
    }

    fun fightDailyCap(familiarId: Int): Int =
        getFightInfo(familiarId)?.dailyCap ?: 0

    private fun trackerValue(prefKey: String, preferences: Preferences?): Int =
        preferences?.getInt(prefKey, 0) ?: 0
}
