package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.preferences.Preferences

/** Counter recognition for the Cursed Magnifying Glass. */
object CursedMagnifyingGlassManager {
    private val messages = listOf(
        "In the distance, an owl hoots 13 times.  Give it a rest, Mr. Owl." to 1,
        "You are startled by the cacophanous cawing of a bunch of crows.  Probably exactly twelve crows, if you had to guess." to 2,
        "A distant clock chimes 11, even though it is (probably) not 11 o'clock right now." to 3,
        "A madman in the distance shrieks: &quot;Ten!  Only ten now!  Hee hee!&quot;" to 4,
        "Nine ravens burst from a nearby tree and take to the sky." to 5,
        "Eight rats scurry out from behind a nearby bush, startling you." to 6,
        "To your left, seven stray dogs fight over a scrap of carrion." to 7,
        "A creepy-looking little girl walks up and whispers in your ear.  &quot;Six.&quot;" to 8,
        "You look at your left hand and notice, to your horror, that you have five fingers.  Oh, wait, that's the normal number.  Never mind." to 9,
        "The bells of a distant cathedral ring four times." to 10,
        "Three wolves howl in the distance.  You wonder what they're howling about.  Probably just ordinary wolf stuff." to 11,
        "You hear two black cats fighting somewhere nearby.  At least you hope they're fighting." to 12,
        "The hair on the back of your neck stands up.  A feeling of impending dread overwhelms your senses." to 13,
    )

    fun updatePreference(resultText: String?, preferences: Preferences?): Boolean {
        if (resultText.isNullOrBlank() || preferences == null) return false
        val count = messages.firstOrNull { resultText.contains(it.first) }?.second
            ?: return false
        preferences.setInt("cursedMagnifyingGlassCount", count)
        return true
    }

    fun reset(preferences: Preferences?) {
        preferences?.setInt("cursedMagnifyingGlassCount", 0)
    }

    fun hasGlass(itemCount: (Int) -> Int): Boolean =
        itemCount(ItemPool.CURSED_MAGNIFYING_GLASS) > 0
}
