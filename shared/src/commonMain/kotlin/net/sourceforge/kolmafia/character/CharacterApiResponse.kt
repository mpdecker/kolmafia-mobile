package net.sourceforge.kolmafia.character

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Models the JSON returned by https://www.kingdomofloathing.com/api.php?what=status
// All numeric fields come back as strings. Fields absent from the API response
// default to the values declared here.
@Serializable
data class CharacterApiResponse(

    // ── Identity ──────────────────────────────────────────────────────────────
    val name: String = "",
    val playerid: String = "0",
    val level: String = "1",
    @SerialName("class") val classId: String = "0",
    val sign: String = "",
    val path: String = "",
    val ascensions: String = "0",
    val gender: String = "",               // "male" / "female"
    val title: String = "",

    // ── HP / MP ──────────────────────────────────────────────────────────────
    val hp: String = "0",
    val hpmax: String = "0",
    val basehpmax: String = "0",           // HP max before equipment/effects
    val mp: String = "0",
    val mpmax: String = "0",
    val basempmax: String = "0",           // MP max before equipment/effects

    // ── Stats ─────────────────────────────────────────────────────────────────
    val mus: String = "0",
    val musexp: String = "0",              // total subpoints (not just since-last-gain)
    val mys: String = "0",
    val mysexp: String = "0",
    val mox: String = "0",
    val moxexp: String = "0",
    // Buffed values (equipment + effects applied)
    val buffedmus: String = "0",
    val buffedmys: String = "0",
    val buffedmox: String = "0",

    // ── Currency ─────────────────────────────────────────────────────────────
    val meat: String = "0",
    val storagemeat: String = "0",         // meat in Hagnk's storage

    // ── Turn / day counters ───────────────────────────────────────────────────
    val adventures: String = "0",
    val turnsplayed: String = "0",         // all-time across ascensions
    val currentrun: String = "0",          // turns this ascension
    val daycount: String = "0",            // days into this ascension
    val rollover: String = "0",            // Unix epoch of next rollover

    // ── Consumables ───────────────────────────────────────────────────────────
    val fullness: String = "0",
    val drunk: String = "0",
    val spleen: String = "0",
    val stomachsize: String = "15",
    val liversize: String = "14",
    val spleensize: String = "15",

    // ── PvP ──────────────────────────────────────────────────────────────────
    val pvpfights: String = "0",

    // ── Ascension / run mode ─────────────────────────────────────────────────
    val roninleft: String = "0",
    val hardcore: String = "0",
    val limitmode: String = "",
    val kingliberated: String = "0",

    // ── Class-specific combat resources ──────────────────────────────────────
    val fury: String = "0",                // Seal Clubber
    val soulsauce: String = "0",           // Sauceror
    val thunder: String = "0",             // Plumber
    val rain: String = "0",                // Plumber
    val lightning: String = "0",           // Plumber
    val pp: String = "0",                  // Plumber power points (current)
    val ppmax: String = "0",               // Plumber power points (max)
    val robonenergy: String = "0",         // You, Robot energy
    val robonscraps: String = "0",         // You, Robot scraps
    val wildfirewater: String = "0",       // Wildfire water
    val minstrel: String = "0",            // Avatar of Boris minstrel level

    // ── Social / access flags ─────────────────────────────────────────────────
    val hasstore: String = "0",
    val hasdisplaycase: String = "0",
    val hasclan: String = "0",
    val hippystone: String = "0",          // 1 = broken (PvP enabled)

    // ── Campground ────────────────────────────────────────────────────────────
    val telescopelevel: String = "0",

    // ── Familiar ─────────────────────────────────────────────────────────────
    val familiar: String = "0",            // familiar type id
    val familiarname: String = "",
    val familiarweight: String = "0",
    val familiarexp: String = "0",
    val enthroned: String = "0",           // enthroned familiar id
    val enthronedname: String = "",
    val bjorned: String = "0",             // bjorned familiar id
    val bjornedname: String = "",

    // ── Moon / KoL calendar ──────────────────────────────────────────────────
    val moonphase: String = "0",
    val moonsign: String = "0",
    val moonday: String = "0",

    // ── Misc ─────────────────────────────────────────────────────────────────
    val mcd: String = "0",                 // mind control device level
    val radsickness: String = "0",
    val stills: String = "-1",             // cocktailcrafting stills; -1 = unknown
    val arenawins: String = "0",

    // ── Equipment ────────────────────────────────────────────────────────────
    val hat: String = "",
    val weapon: String = "",
    val offhand: String = "",
    val shirt: String = "",
    val pants: String = "",
    val acc1: String = "",
    val acc2: String = "",
    val acc3: String = "",
    val familiarequip: String = "",
    val container: String = ""
)
