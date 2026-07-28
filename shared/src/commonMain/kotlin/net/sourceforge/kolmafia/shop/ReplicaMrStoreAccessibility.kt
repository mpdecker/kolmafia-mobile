package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop ReplicaMrStoreRequest.availableItem year gates. */
object ReplicaMrStoreAccessibility {

    private val ITEM_TO_YEAR = mapOf(
        11190 to 2004, // replica Dark Jill-O-Lantern
        11191 to 2004, // replica hand turkey outline
        11192 to 2004, // replica crimbo elfling
        11195 to 2005, // replica miniature gravy-covered maypole
        11196 to 2005, // replica wax lips
        11193 to 2005, // replica pygmy bugbear shaman
        11197 to 2006, // replica Tome of Snowcone Summoning
        11199 to 2006, // replica jewel-eyed wizard hat
        11252 to 2006, // replica plastic pumpkin bucket
        11200 to 2007, // replica bottle-rocket crossbow
        11201 to 2007, // replica navel ring of navel gazing
        11202 to 2007, // replica V for Vivala mask
        11204 to 2008, // replica little box of fireworks
        11205 to 2008, // replica cotton candy cocoon
        11203 to 2008, // replica haiku katana
        11218 to 2009, // replica Apathargic Bandersnatch
        11206 to 2009, // replica Elvish sunglasses
        11207 to 2009, // replica squamous polyp
        11211 to 2010, // replica Juju Mojo Mask
        11209 to 2010, // replica Greatest American Pants
        11210 to 2010, // replica organ grinder
        11215 to 2011, // replica cute angel
        11212 to 2011, // replica Operation Patriot Shield
        11214 to 2011, // replica plastic vampire fangs
        11213 to 2012, // replica Libram of Resolutions
        11216 to 2012, // replica Camp Scout backpack
        11217 to 2012, // replica deactivated nanobots
        11221 to 2013, // replica Order of the Green Thumb Order Form
        11220 to 2013, // replica over-the-shoulder Folder Holder
        11219 to 2013, // replica Smith's Tome
        11225 to 2014, // replica Little Geneticist DNA-Splicing Lab
        11226 to 2014, // replica still grill
        11227 to 2014, // replica Crimbo sapling
        11229 to 2015, // replica Chateau Mantegna room key
        11228 to 2015, // replica yellow puck
        11230 to 2015, // replica Deck of Every Card
        11233 to 2016, // replica Witchess Set
        11232 to 2016, // replica disconnected intergnat
        11231 to 2016, // replica Source terminal
        11235 to 2017, // replica space planula
        11236 to 2017, // replica unpowered Robortender
        11234 to 2017, // replica genie bottle
        11238 to 2018, // replica January's Garbage Tote
        11239 to 2018, // replica God Lobster Egg
        11237 to 2018, // replica Neverending Party invitation envelope
        11241 to 2019, // replica Kramco Sausage-o-Matic
        11240 to 2019, // replica Fourth of May Cosplay Saber
        11242 to 2019, // replica hewn moon-rune spoon
        11244 to 2020, // replica Powerful Glove
        11243 to 2020, // replica baby camelCalf
        11245 to 2020, // replica Cargo Cultist Shorts
        11247 to 2021, // replica miniature crystal ball
        11248 to 2021, // replica emotion chip
        11246 to 2021, // replica industrial fire extinguisher
        11250 to 2022, // replica grey gosling
        11251 to 2022, // replica designer sweatpants
        11249 to 2022, // replica Jurassic Parka
        11254 to 2023, // replica Cincho de Mayo
        11280 to 2023, // Replica 2002 Mr. Store Catalog
        11304 to 2023, // replica sleeping patriotic eagle
        11325 to 2023, // replica august scepter
    )

    private val FREE_YEARS = setOf(2023)

    fun isItemAvailable(
        itemId: Int,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
    ): Boolean {
        val itemYear = ITEM_TO_YEAR[itemId] ?: return false
        val currentYear = prefs?.getInt("currentReplicaStoreYear", 2004) ?: 2004
        if (itemYear == currentYear) return true
        if (itemYear in FREE_YEARS &&
            accessibleCount(itemId) <= 0
        ) {
            return true
        }
        return false
    }
}
