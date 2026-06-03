package net.sourceforge.kolmafia.adventure.choice

import net.sourceforge.kolmafia.adventure.choice.solvers.ArcadeGameSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.GameproSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.LightsOutSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.LostKeySolver
import net.sourceforge.kolmafia.adventure.choice.solvers.SafetyShelterSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.VampOutSolver

data class ChoiceSolvers(
    val safetyShelter: SafetyShelterSolver,
    val vampOut: VampOutSolver,
    val arcadeGame: ArcadeGameSolver,
    val lostKey: LostKeySolver,
    val gamepro: GameproSolver,
    val lightsOut: LightsOutSolver,
)
