package net.sourceforge.kolmafia.adventure.choice.solvers

import net.sourceforge.kolmafia.preferences.Preferences

class LightsOutSolverImpl(private val preferences: Preferences) : LightsOutSolver {

    override fun autoLightsOut(choiceId: Int, responseText: String): Int? {
        val automation = preferences.getInt("lightsOutAutomation", 0)
        if (automation == 0) return null
        return autoByRoom(choiceId, responseText, automation)
    }

    private fun autoByRoom(choiceId: Int, responseText: String, automation: Int): Int = when (choiceId) {
        890 -> if (automation == 1 && responseText.contains("Look Out the Window")) 3 else 1
        891 -> if (automation == 1 && responseText.contains("Check a Pile of Stained Sheets")) 3 else 1
        892 -> if (automation == 1 && responseText.contains("Inspect the Bathtub")) 3 else 1
        893 -> if (automation == 1 && responseText.contains("Make a Snack")) 4 else 1
        894 -> if (automation == 1 && responseText.contains("Go to the Children's Section")) 2 else 1
        895 -> if (automation == 1 && responseText.contains("Dance with Yourself")) 2 else 1
        896 -> if (automation == 1 && responseText.contains("Check out the Tormented Damned Souls Painting")) 4 else 1
        897 -> when {
            responseText.contains("Search for a light")          -> if (automation == 1) 1 else 2
            responseText.contains("Search a nearby nightstand")  -> 3
            responseText.contains("Check a nightstand on your left") -> 1
            else -> 2
        }
        898 -> when {
            responseText.contains("Search for a lamp")                           -> if (automation == 1) 1 else 2
            responseText.contains("Search over by the (gaaah) stuffed animals") -> 2
            responseText.contains("Examine the Dresser")                         -> 2
            responseText.contains("Open the bear and put your hand inside")      -> 1
            responseText.contains("Unlock the box")                              -> 1
            else -> 2
        }
        899 -> when {
            responseText.contains("Make a torch")                        -> if (automation == 1) 1 else 2
            responseText.contains("Examine the Graves")                  -> 2
            responseText.contains("Examine the grave marked \"Crumbles\"") -> 2
            else -> 2
        }
        900 -> when {
            responseText.contains("Search for a light")              -> if (automation == 1) 1 else 2
            responseText.contains("What the heck, let's explore a bit") -> 2
            responseText.contains("Examine the taxidermy heads")        -> 2
            else -> 2
        }
        901 -> when {
            responseText.contains("Try to find a light")        -> if (automation == 1) 1 else 2
            responseText.contains("Keep your cool")             -> 2
            responseText.contains("Investigate the wine racks") -> 2
            responseText.contains("Examine the Pinot Noir rack") -> 3
            else -> 2
        }
        902 -> when {
            responseText.contains("Look for a light")      -> if (automation == 1) 1 else 2
            responseText.contains("Search the barrel")     -> 2
            responseText.contains("No, but I will anyway") -> 2
            else -> 2
        }
        903 -> when {
            responseText.contains("Search for a light")                   -> if (automation == 1) 1 else 2
            responseText.contains("Check it out")                          -> 1
            responseText.contains("Examine the weird machines")            -> 3
            responseText.contains("Enter 23-47-99 and turn on the machine") -> 1
            responseText.contains("Oh god")                                -> 1
            else -> 2
        }
        else -> 2
    }
}
