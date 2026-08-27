package net.sourceforge.kolmafia.adventure

/**
 * Desktop [AdventureRequest.ShadowRift] — 13 ingresses into snarfblat 567
 * (Phases 2571–2585).
 */
enum class ShadowRift(
    val container: String,
    val place: String,
    val action: String,
) {
    BEACH("Desert Beach", "desertbeach", "db_shadowrift"),
    VILLAGE("Forest Village", "forestvillage", "fv_shadowrift"),
    MCLARGEHUGE("Mt. McLargeHuge", "mclargehuge", "mcl_shadowrift"),
    BEANSTALK("Somewhere Over the Beanstalk", "beanstalk", "stalk_rift"),
    MANOR("Spookyraven Manor Third Floor", "manor3", "manor3_shadowrift"),
    REALM("The 8-Bit Realm", "8bit", "8rift"),
    PYRAMID("The Ancient Buried Pyramid", "pyramid", "pyramid_shadowrift"),
    CASTLE("The Castle in the Clouds in the Sky", "giantcastle", "castle_shadowrift"),
    WOODS("The Distant Woods", "woods", "woods_shadowrift"),
    CITY("The Hidden City", "hiddencity", "hc_shadowrift"),
    CEMETARY("The Misspelled Cemetary", "cemetery", "cem_shadowrift"),
    PLAINS("The Nearby Plains", "plains", "plains_shadowrift"),
    TOWN("The Right Side of the Tracks", "town_right", "townright_shadowrift");

    val adventureName: String = "Shadow Rift ($container)"
    val url: String = "place.php?whichplace=$place&action=$action"
    val freeAction: String = "${action}_free"
    val freeUrl: String = "place.php?whichplace=$place&action=$freeAction"

    fun currentAction(hasShadowAffinity: Boolean): String =
        if (hasShadowAffinity) freeAction else action

    fun currentUrl(hasShadowAffinity: Boolean): String =
        if (hasShadowAffinity) freeUrl else url

    companion object {
        const val SNARFBLAT = "567"
        const val ADVENTURE_ID = "shadow_rift"
        const val INGRESS_PREF = "shadowRiftIngress"
        const val SHADOW_AFFINITY = "Shadow Affinity"

        private val byPlace: Map<String, ShadowRift> =
            entries.associateBy { it.place.lowercase() }
        private val byAdventureName: Map<String, ShadowRift> =
            entries.associateBy { it.adventureName.lowercase() }

        fun findPlace(place: String): ShadowRift? =
            byPlace[place.lowercase()]

        fun findAdventureName(adventureName: String): ShadowRift? =
            byAdventureName[adventureName.lowercase()]
    }
}
