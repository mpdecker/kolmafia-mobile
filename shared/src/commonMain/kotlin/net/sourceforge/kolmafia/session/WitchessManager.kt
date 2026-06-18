package net.sourceforge.kolmafia.session

import io.ktor.client.HttpClient
import net.sourceforge.kolmafia.data.WitchessSolutionDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.WitchessRequest

object WitchessManager {

    suspend fun solveDailyPuzzles(
        client: HttpClient,
        preferences: Preferences,
        request: WitchessRequest = WitchessRequest(client),
        print: (String) -> Unit,
    ) {
        val campground = request.visitPage("campground.php").getOrElse {
            print("Failed to visit campground.")
            return
        }
        if (!campground.contains("chesstable.gif")) {
            print("You don't have a Witchess Set installed in your campground.")
            return
        }

        request.visitPage("campground.php?action=witchess").onFailure {
            print("Failed to open Witchess set.")
            return
        }
        request.visitPage("choice.php?whichchoice=1181&option=3").onFailure {
            print("Failed to navigate Witchess choices.")
            return
        }
        request.visitPage("choice.php?whichchoice=1183&option=2").onFailure {
            print("Failed to navigate Witchess choices.")
            return
        }

        val solvedPuzzles = mutableListOf<String>()
        for (slot in 1..5) {
            val puzzleResult = request.fetchPuzzle(slot.toString(), preferences, errorOnNoRun = true)
                .getOrElse { error ->
                    print(error.message ?: "Failed to fetch Witchess puzzle.")
                    return
                }

            val puzzleId = puzzleResult.puzzleId
            if (puzzleId < 0) {
                print(puzzleResult.responseText)
                return
            }

            if (puzzleResult.isSolved) {
                print("Already solved Witchess Puzzle #$puzzleId.")
                solvedPuzzles.add(puzzleId.toString())
                continue
            }

            val solution = WitchessSolutionDatabase.getWitchessSolution(puzzleId)
            if (solution == null) {
                print("No solution on file for Witchess Puzzle #$puzzleId.")
                return
            }

            print("Attempting to solve Witchess Puzzle #$puzzleId...")
            val solved = request.submitSolution(puzzleId, solution.coords).getOrElse {
                print("Failed to submit Witchess solution.")
                return
            }
            if (solved) {
                print("Solved!")
                solvedPuzzles.add(puzzleId.toString())
            } else {
                print(
                    "Failed to solve the Witchess Puzzle for some reason. " +
                        "If this happens again, please file a bug report.",
                )
                return
            }
        }

        if (solvedPuzzles.isNotEmpty()) {
            print("Solved daily Witchess puzzles: ${solvedPuzzles.joinToString(", ")}")
        }
    }
}
