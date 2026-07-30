package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.math.pow

/**
 * Bastille Battalion style-set stat bonuses from [bastille.txt].
 * Mirrors desktop [BastilleBattalionManager] style-set key math.
 */
@OptIn(ExperimentalResourceApi::class)
object BastilleDatabase {

    enum class Stat(val index: Int) {
        MA(0),
        MD(1),
        CA(2),
        CD(3),
        PA(4),
        PD(5),
        NONE(6),
        ;

        val code: String get() = name
    }

    data class Stats(
        val ma: Int = 0,
        val md: Int = 0,
        val ca: Int = 0,
        val cd: Int = 0,
        val pa: Int = 0,
        val pd: Int = 0,
    ) {
        fun get(stat: Stat): Int = when (stat) {
            Stat.MA -> ma
            Stat.MD -> md
            Stat.CA -> ca
            Stat.CD -> cd
            Stat.PA -> pa
            Stat.PD -> pd
            Stat.NONE -> 0
        }

        fun toSetting(): String =
            Stat.entries
                .filter { it != Stat.NONE }
                .joinToString(",") { "${it.code}=${get(it)}" }

        fun toStrengthString(): String =
            "Military ${ma}/${md} Castle ${ca}/${cd} Psychological ${pa}/${pd}"
    }

    enum class Upgrade(val option: Int, val prefix: String) {
        BARBICAN(1, "barb"),
        DRAWBRIDGE(2, "bridge"),
        MURDER_HOLES(3, "holes"),
        MOAT(4, "moat"),
        ;

        val digitIndex: Int get() = 4 - option
        val scale: Int get() = 3.0.pow(digitIndex.toDouble()).toInt()
    }

    enum class Style(val displayName: String, val index: Int, val upgrade: Upgrade) {
        BARBECUE("Barbarian Barbecue", 1, Upgrade.BARBICAN),
        BABAR("Babar", 2, Upgrade.BARBICAN),
        BARBERSHOP("Barbershop", 3, Upgrade.BARBICAN),

        BRUTALIST("Brutalist", 1, Upgrade.DRAWBRIDGE),
        DRAFTSMAN("Draftsman", 2, Upgrade.DRAWBRIDGE),
        NOUVEAU("Art Nouveau", 3, Upgrade.DRAWBRIDGE),

        CANNON("Cannon", 1, Upgrade.MURDER_HOLES),
        CATAPULT("Catapult", 2, Upgrade.MURDER_HOLES),
        GESTURE("Gesture", 3, Upgrade.MURDER_HOLES),

        SHARKS("Sharks", 1, Upgrade.MOAT),
        LAVA("Lava", 2, Upgrade.MOAT),
        TRUTH("Truth Serum", 3, Upgrade.MOAT),
        ;

        val image: String get() = "${upgrade.prefix}$index.png"
        val scaledDigit: Int get() = (index - 1) * upgrade.scale

        override fun toString(): String = "${upgrade.name.replace('_', ' ')} $displayName"
    }

    enum class Castle(val prefix: String, val description: String, val superiorStat: Stat) {
        ART("frenchcastle", "an avant-garde art castle", Stat.PD),
        BORING("masterofnone", "a boring, run-of-the-mill castle", Stat.MD),
        CHATEAU("bigcastle", "a sprawling chateau", Stat.CA),
        CITADEL("berserker", "a dark and menacing citadel", Stat.PA),
        FORTIFIED("shieldmaster", "a fortress that puts the 'fort' in 'fortified'", Stat.CD),
        MILITARY("barracks", "an imposing military fortress", Stat.MA),
        ;

        companion object {
            private val byDescription = entries.associateBy { it.description }
            private val byImagePrefix = entries.flatMap { castle ->
                listOf("${castle.prefix}_1.png", "${castle.prefix}_2.png", "${castle.prefix}_3.png")
                    .map { img -> img to castle }
            }.toMap()

            fun fromDescription(text: String): Castle? = byDescription[text]
            fun fromBattleImage(image: String): Castle? = byImagePrefix[image]
        }
    }

    private val styleSetToStats = mutableMapOf<Int, Stats>()
    private var loaded = false

    val isLoaded: Boolean get() = loaded
    val styleSetCount: Int get() = styleSetToStats.size

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/bastille.txt").decodeToString()
        applyParse(parse(text))
        loaded = true
    }

    fun statsForKey(key: Int): Stats? = styleSetToStats[key]

    fun statsForStyles(styles: Collection<Style>): Stats? =
        statsForKey(stylesToKey(styles))

    fun predictedStats(currentStyles: Map<Upgrade, Style>): Stats? =
        statsForStyles(currentStyles.values)

    fun stylesToKey(styles: Collection<Style>): Int =
        styles.sumOf { it.scaledDigit }

    fun stylesToKey(vararg styles: Style): Int = stylesToKey(styles.toList())

    fun keyToStyles(key: Int): Set<Style> {
        val digits = intArrayOf(
            key % 3,
            (key / 3) % 3 * 3,
            (key / 9) % 3 * 9,
            (key / 27) % 3 * 27,
        )
        return Style.entries.filter { style ->
            digits[style.upgrade.digitIndex] == style.scaledDigit
        }.toSet()
    }

    fun styleFromImage(image: String): Style? =
        Style.entries.firstOrNull { it.image == image }

    fun upgradeForOption(option: Int): Upgrade? =
        Upgrade.entries.firstOrNull { it.option == option }

    internal fun parseForTest(text: String): ParseSnapshot = parse(text)

    internal fun injectForTest(snapshot: ParseSnapshot) {
        styleSetToStats.clear()
        styleSetToStats.putAll(snapshot.statsByKey)
        loaded = true
    }

    internal fun resetForTest() {
        styleSetToStats.clear()
        loaded = false
    }

    data class ParseSnapshot(val statsByKey: Map<Int, Stats>)

    private fun applyParse(snapshot: ParseSnapshot) {
        styleSetToStats.clear()
        styleSetToStats.putAll(snapshot.statsByKey)
    }

    private fun parse(text: String): ParseSnapshot {
        val stats = mutableMapOf<Int, Stats>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val parts = line.split('\t')
            if (parts.size < 11) continue
            val key = parts[0].trim().toIntOrNull()?.minus(1) ?: continue
            if (key !in 0..80 || stats.containsKey(key)) continue
            stats[key] = Stats(
                ma = parts[5].trim().toIntOrNull() ?: 0,
                md = parts[6].trim().toIntOrNull() ?: 0,
                ca = parts[7].trim().toIntOrNull() ?: 0,
                cd = parts[8].trim().toIntOrNull() ?: 0,
                pa = parts[9].trim().toIntOrNull() ?: 0,
                pd = parts[10].trim().toIntOrNull() ?: 0,
            )
        }
        return ParseSnapshot(stats)
    }
}
