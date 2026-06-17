package net.sourceforge.kolmafia.adventure

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

data class TowerDoorLock(
    val name: String,
    val keyItemId: Int?,
    val keyName: String,
    val action: String,
    val special: Boolean = false,
    val locationName: String? = null,
    val encounterName: String? = null,
) {
    val isDoorknob: Boolean get() = keyItemId == null
    val requiresAdventure: Boolean get() = locationName != null
}

object TowerDoorConfig {

    const val DOOR_PLACE = "nstower_door"
    const val LOW_KEY_DOOR_PLACE = "nstower_doorlowkey"
    const val KEYS_USED_PREF = "nsTowerDoorKeysUsed"
    const val LOCK_PICKING_SKILL = 195
    const val LOCK_PICKING_CHOICE = 1414

    val STANDARD_LOCKS: List<TowerDoorLock> = listOf(
        TowerDoorLock("Boris's Lock", ItemPool.BORIS_KEY, "Boris's key", "ns_lock1", special = true),
        TowerDoorLock("Jarlsberg's Lock", ItemPool.JARLSBERG_KEY, "Jarlsberg's key", "ns_lock2", special = true),
        TowerDoorLock("Sneaky Pete's Lock", ItemPool.SNEAKY_PETE_KEY, "Sneaky Pete's key", "ns_lock3", special = true),
        TowerDoorLock("Star Lock", ItemPool.STAR_KEY, "Richard's star key", "ns_lock4"),
        TowerDoorLock("Digital Lock", ItemPool.DIGITAL_KEY, "digital key", "ns_lock5", special = true),
        TowerDoorLock("Skeleton Lock", ItemPool.SKELETON_KEY, "skeleton key", "ns_lock6"),
        TowerDoorLock("Doorknob", null, "", "ns_doorknob"),
    )

    val LOW_KEY_LOCKS: List<TowerDoorLock> = listOf(
        TowerDoorLock("Boris's Lock", ItemPool.BORIS_KEY, "Boris's key", "ns_lock1_lk"),
        TowerDoorLock("Jarlsberg's Lock", ItemPool.JARLSBERG_KEY, "Jarlsberg's key", "ns_lock2_lk"),
        TowerDoorLock("Sneaky Pete's Lock", ItemPool.SNEAKY_PETE_KEY, "Sneaky Pete's key", "ns_lock3_lk"),
        TowerDoorLock("Star Lock", ItemPool.STAR_KEY, "Richard's star key", "ns_lock4_lk"),
        TowerDoorLock("Digital Lock", ItemPool.DIGITAL_KEY, "digital key", "ns_lock5_lk"),
        TowerDoorLock("Skeleton Lock", ItemPool.SKELETON_KEY, "skeleton key", "ns_lock6_lk"),
        TowerDoorLock("Doorknob", null, "", "ns_doorknob_lk"),
        lowKeyAdventureLock("Polka Dotted Lock", ItemPool.CLOWN_CAR_KEY, "clown car key", "key1", "The \"Fun\" House", "Carpool Lane"),
        lowKeyAdventureLock("Bat-Winged Lock", ItemPool.BATTING_CAGE_KEY, "batting cage key", "key2", "Bat Hole Entrance", "Batting a Thousand"),
        lowKeyAdventureLock("Taco Locko", ItemPool.AQUI, "AQUI", "key3", "South of the Border", "Lost in Translation"),
        lowKeyAdventureLock("Lockenmeyer Flask", ItemPool.KNOB_LABINET_KEY, "Knob lab cabinet key", "key4", "Cobb's Knob Laboratory", "F=ma"),
        lowKeyAdventureLock("Antlered Lock", ItemPool.WEREMOOSE_KEY, "weremoose key", "key5", "Cobb's Knob Menagerie, Level 2", "It's a Weremoose Key"),
        lowKeyAdventureLock("Lock with one Eye", ItemPool.PEG_KEY, "peg key", "key6", "The Obligatory Pirate's Cove", "Larrrst & Found"),
        lowKeyAdventureLock("Trolling Lock", ItemPool.KEKEKEY, "Kekekey", "key7", "The Valley of Rof L'm Fao", "Made of Stars"),
        lowKeyAdventureLock("Rabbit-Eared Lock", ItemPool.RABBITS_FOOT_KEY, "rabbit's foot key", "key8", "The Dire Warren", "Lucky For You"),
        lowKeyAdventureLock("Mine Cart Shaped Lock", ItemPool.KNOB_SHAFT_SKATE_KEY, "Knob shaft skate key", "key9", "The Knob Shaft", "Getting the Shaft Key"),
        lowKeyAdventureLock("Frigid Lock", ItemPool.ICE_KEY, "ice key", "key10", "The Icy Peak", "OF Ice & Yetis"),
        lowKeyAdventureLock("Anchovy Can", ItemPool.ANCHOVY_CAN_KEY, "anchovy can key", "key11", "The Haunted Pantry", "Hola, Amigos"),
        lowKeyAdventureLock("Cactus-Shaped-Hole Lock", ItemPool.CACTUS_KEY, "cactus key", "key12", "The Arid, Extra-Dry Desert", "Midnight Sun"),
        lowKeyAdventureLock("Boat Prow Lock", ItemPool.F_C_LE_SH_C_LE_K_Y, "F'c'le sh'c'le k'y", "key13", "The F'c'le", "B'c'le Up"),
        lowKeyAdventureLock("Barnacley Lock", ItemPool.TREASURE_CHEST_KEY, "treasure chest key", "key14", "Belowdecks", "Yo-ho-ho in the Ho-ho-hold"),
        lowKeyAdventureLock("Infernal Lock", ItemPool.DEMONIC_KEY, "demonic key", "key15", "Pandamonium Slums", "You Found a Thing, in Hell"),
        lowKeyAdventureLock("Sausage With a Hole", ItemPool.KEY_SAUSAGE, "key sausage", "key16", "Cobb's Knob Kitchens", "Pork Key's Revenge"),
        lowKeyAdventureLock("Golden Lock", ItemPool.KNOB_TREASURY_KEY, "Knob treasury key", "key17", "Cobb's Knob Treasury", "Decisions, Shmecisions"),
        lowKeyAdventureLock("Junky Lock", ItemPool.SCRAP_METAL_KEY, "scrap metal key", "key18", "The Old Landfill", "One Man's Trash is Presumably the Key to Another Man's Treasure"),
        lowKeyAdventureLock("Spooky Lock", ItemPool.BLACK_ROSE_KEY, "black rose key", "key19", "The Haunted Conservatory", "Lore of the Roses"),
        lowKeyAdventureLock("Crib-Shaped Lock", ItemPool.MUSIC_BOX_KEY, "music box key", "key20", "The Haunted Nursery", "Jerk-in-the-Box"),
        lowKeyAdventureLock("Boney Lock", ItemPool.ACTUAL_SKELETON_KEY, "actual skeleton key", "key21", "The Skeleton Store", "Just Taking the Opportunity to be a Pedant"),
        lowKeyAdventureLock("Loaf of Bread with Keyhole", ItemPool.DEEP_FRIED_KEY, "deep-fried key", "key22", "Madness Bakery", "Into the Fryer"),
        lowKeyAdventureLock("Overgrown Lock", ItemPool.DISCARDED_BIKE_LOCK_KEY, "discarded bike lock key", "key23", "The Overgrown Lot", "Not a Lot Left"),
    )

    private val actionToLock: Map<String, TowerDoorLock> =
        (STANDARD_LOCKS + LOW_KEY_LOCKS).associateBy { it.action }

    private fun lowKeyAdventureLock(
        name: String,
        itemId: Int,
        keyName: String,
        actionSuffix: String,
        location: String,
        encounter: String,
    ): TowerDoorLock = TowerDoorLock(
        name = name,
        keyItemId = itemId,
        keyName = keyName,
        action = "nstower_doowlow$actionSuffix",
        locationName = location,
        encounterName = encounter,
    )

    fun locksFor(character: CharacterState): List<TowerDoorLock> =
        if (character.isLowkey) LOW_KEY_LOCKS else STANDARD_LOCKS

    fun doorPlaceFor(character: CharacterState): String =
        if (character.isLowkey) LOW_KEY_DOOR_PLACE else DOOR_PLACE

    fun findLockByAction(action: String): TowerDoorLock? = actionToLock[action]

    fun parseTowerDoor(html: String, locks: List<TowerDoorLock> = STANDARD_LOCKS): String {
        val buffer = StringBuilder()
        for (lock in locks) {
            if (lock.isDoorknob) continue
            if (!html.contains("${lock.action} ")) {
                if (buffer.isNotEmpty()) buffer.append(",")
                buffer.append(lock.keyName)
            }
        }
        return buffer.toString()
    }

    fun syncTowerDoorFromHtml(html: String, preferences: Preferences, locks: List<TowerDoorLock> = STANDARD_LOCKS) {
        preferences.setString(KEYS_USED_PREF, parseTowerDoor(html, locks))
    }

    fun keysUsed(preferences: Preferences): Set<String> =
        preferences.getString(KEYS_USED_PREF, "")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    fun isKeyUsed(preferences: Preferences, keyName: String): Boolean =
        preferences.getString(KEYS_USED_PREF, "").contains(keyName)

    fun appendKeyUsed(preferences: Preferences, keyName: String) {
        if (keyName.isBlank() || isKeyUsed(preferences, keyName)) return
        val keys = preferences.getString(KEYS_USED_PREF, "")
        preferences.setString(
            KEYS_USED_PREF,
            if (keys.isEmpty()) keyName else "$keys,$keyName",
        )
    }

    fun neededLocks(preferences: Preferences, locks: List<TowerDoorLock> = STANDARD_LOCKS): List<TowerDoorLock> =
        locks.filter { lock ->
            !lock.isDoorknob && !isKeyUsed(preferences, lock.keyName)
        }

    fun isUnlockSuccess(html: String): Boolean =
        html.contains("the lock vanishes", ignoreCase = true) ||
            html.contains("turn back to the lock", ignoreCase = true) ||
            html.contains("the lock is gone", ignoreCase = true) ||
            html.contains("crumble to dust", ignoreCase = true) ||
            html.contains("the lock disappears", ignoreCase = true) ||
            html.contains("the lock and the key disappear", ignoreCase = true)

    fun towerDoorErrorMessage(status: String, questDatabase: QuestDatabase): String = when {
        status == QuestDatabase.UNSTARTED ->
            "You haven't been given the quest to fight the Sorceress!"
        questDatabase.isQuestLaterThan(Quest.FINAL, "step5") ->
            "You have already opened the Tower Door."
        else ->
            "You haven't reached the Tower Door yet."
    }

    fun extractDoorAction(url: String): String? =
        Regex("""action=([^&]+)""", RegexOption.IGNORE_CASE)
            .find(url)
            ?.groupValues
            ?.get(1)

    fun legendKeyPickOption(keyItemId: Int?): Int? = when (keyItemId) {
        ItemPool.BORIS_KEY -> 1
        ItemPool.JARLSBERG_KEY -> 2
        ItemPool.SNEAKY_PETE_KEY -> 3
        else -> null
    }

    fun adventureKeyErrorMessage(lock: TowerDoorLock): String {
        val keyLabel = lock.keyName.ifBlank { lock.name }
        return "Adventure in ${lock.locationName} until you find a $keyLabel"
    }
}
