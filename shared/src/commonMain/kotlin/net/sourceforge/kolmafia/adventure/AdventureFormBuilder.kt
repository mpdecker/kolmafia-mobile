package net.sourceforge.kolmafia.adventure

/**
 * Resolved form POST target for a single adventure turn
 * (desktop AdventureRequest formSource + adventureId).
 */
data class AdventureForm(
    val formSource: String,
    val fields: Map<String, String>,
    /** Canonical request URL for logging / session (path + query). */
    val requestUrl: String,
) {
    companion object {
        fun adventurePhp(snarfblat: String): AdventureForm =
            AdventureForm(
                formSource = "adventure.php",
                fields = mapOf("snarfblat" to snarfblat, "adv" to "1"),
                requestUrl = "adventure.php?snarfblat=$snarfblat",
            )
    }
}

/**
 * Builds desktop-parity AdventureRequest form fields from formSource + adventureId
 * (Phases 2601–2615).
 */
object AdventureFormBuilder {

    data class Context(
        val adventureName: String = "",
        val preferences: net.sourceforge.kolmafia.preferences.Preferences? = null,
        val hasShadowAffinity: Boolean = false,
        val manorQuestFinished: Boolean = false,
        val cellarSquare: Int = 0,
        val cellarAutoFaucet: Boolean = false,
    )

    fun build(
        formSource: String,
        adventureId: String,
        ctx: Context = Context(),
    ): AdventureForm {
        val source = formSource.ifBlank { "adventure.php" }
        return when (source) {
            "adventure.php" -> AdventureForm.adventurePhp(adventureId)
            "casino.php" -> form(
                "casino.php",
                mapOf("action" to "slot", "whichslot" to adventureId),
            )
            "crimbo10.php" -> form("crimbo10.php", mapOf("place" to adventureId))
            "cobbsknob.php" -> form("cobbsknob.php", mapOf("action" to "throneroom"))
            "friars.php" -> form("friars.php", mapOf("action" to "ritual"))
            "invasion.php" -> form("invasion.php", mapOf("action" to adventureId))
            "mining.php" -> form("mining.php", mapOf("mine" to adventureId))
            "sea_merkin.php" -> seaMerkin(ctx.adventureName)
            "cellar.php" -> cellar(ctx)
            "basement.php" -> form("basement.php", emptyMap())
            "dwarffactory.php" -> form("dwarffactory.php", mapOf("action" to "ware"))
            "place.php" -> placePhp(adventureId, ctx)
            else -> form(source, mapOf("action" to adventureId))
        }
    }

    /** Apply desktop [AdventureRequest.updateFields] mutations. */
    fun updateFields(
        formSource: String,
        adventureId: String,
        ctx: Context,
    ): AdventureForm = when (formSource) {
        "cellar.php" -> cellar(ctx)
        "place.php" -> when (adventureId) {
            "manor4_chamber", "manor4_chamberboss" -> manorChamber(ctx)
            "pyramid_state" -> pyramidState(ctx)
            ShadowRift.ADVENTURE_ID -> shadowRift(ctx)
            else -> build(formSource, adventureId, ctx)
        }
        else -> build(formSource, adventureId, ctx)
    }

    private fun placePhp(adventureId: String, ctx: Context): AdventureForm =
        when (adventureId) {
            "cloudypeak2" -> form(
                "place.php",
                mapOf("whichplace" to "mclargehuge", "action" to adventureId),
            )
            "crimbo22_engine" -> form(
                "place.php",
                mapOf("whichplace" to "crimbo22", "action" to adventureId),
            )
            "ioty2014_wolf" -> form(
                "place.php",
                mapOf("whichplace" to "ioty2014_wolf", "action" to "wolf_houserun"),
            )
            "manor4_chamberboss", "manor4_chamber" -> manorChamber(ctx)
            "ns_01_crowd1", "ns_01_crowd2", "ns_01_crowd3",
            "ns_03_hedgemaze", "ns_05_monster1", "ns_06_monster2",
            "ns_07_monster3", "ns_08_monster4", "ns_09_monster5",
            "ns_10_sorcfight",
            -> form(
                "place.php",
                mapOf("whichplace" to "nstower", "action" to adventureId),
            )
            "pyramid_state" -> pyramidState(ctx)
            ShadowRift.ADVENTURE_ID -> shadowRift(ctx)
            "town_eincursion", "town_eicfight2" -> form(
                "place.php",
                mapOf("whichplace" to "town", "action" to adventureId),
            )
            "townwrong_tunnel" -> form(
                "place.php",
                mapOf("whichplace" to "town_wrong", "action" to "townwrong_tunnel"),
            )
            else -> form(
                "place.php",
                mapOf("whichplace" to adventureId, "action" to adventureId),
            )
        }

    private fun manorChamber(ctx: Context): AdventureForm {
        val action = if (ctx.manorQuestFinished) "manor4_chamber" else "manor4_chamberboss"
        return form("place.php", mapOf("whichplace" to "manor4", "action" to action))
    }

    private fun pyramidState(ctx: Context): AdventureForm {
        val pos = ctx.preferences?.getString("pyramidPosition", "") ?: ""
        val bomb = ctx.preferences?.getBoolean("pyramidBombUsed", false) == true
        val action = buildString {
            append("pyramid_state")
            append(pos)
            if (bomb) append('a')
        }
        return form("place.php", mapOf("whichplace" to "pyramid", "action" to action))
    }

    private fun shadowRift(ctx: Context): AdventureForm {
        val rift = ShadowRift.findAdventureName(ctx.adventureName)
            ?: return AdventureForm.adventurePhp(ShadowRift.SNARFBLAT)
        val current = ctx.preferences?.getString(ShadowRift.INGRESS_PREF, "") ?: ""
        val desired = rift.place
        return if (current.equals(desired, ignoreCase = true)) {
            AdventureForm.adventurePhp(ShadowRift.SNARFBLAT)
        } else {
            ctx.preferences?.setString(ShadowRift.INGRESS_PREF, desired)
            form(
                "place.php",
                mapOf(
                    "whichplace" to rift.place,
                    "action" to rift.currentAction(ctx.hasShadowAffinity),
                ),
            )
        }
    }

    private fun cellar(ctx: Context): AdventureForm =
        if (ctx.cellarAutoFaucet) {
            form("cellar.php", mapOf("action" to "autofaucet"))
        } else {
            val spot = ctx.cellarSquare.takeIf { it > 0 } ?: 1
            form("cellar.php", mapOf("whichspot" to spot.toString(), "action" to "explore"))
        }

    private fun seaMerkin(adventureName: String): AdventureForm {
        val sub = when (adventureName) {
            "Mer-kin Temple (Left Door)" -> "left"
            "Mer-kin Temple (Center Door)" -> "center"
            "Mer-kin Temple (Right Door)" -> "right"
            else -> null
        }
        val fields = mutableMapOf("action" to "temple")
        if (sub != null) fields["subaction"] = sub
        return form("sea_merkin.php", fields)
    }

    private fun form(source: String, fields: Map<String, String>): AdventureForm {
        val query = fields.entries.joinToString("&") { "${it.key}=${it.value}" }
        val url = if (query.isEmpty()) source else "$source?$query"
        return AdventureForm(formSource = source, fields = fields, requestUrl = url)
    }
}
