package net.sourceforge.kolmafia.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DemonName14ManagerTest {

    @Test
    fun canSolveDemonNameWithNoRepetitions() {
        val answer = "MorNixArgPhaDarHutRogBalKru"
        val segments = setOf(
            "Mor", "rNi", "Nix", "xAr", "Arg", "gPh", "Pha", "aDa", "Dar", "arH", "Hut", "utR",
            "Rog", "ogB", "gBa", "alK", "Kru",
        )
        val result = DemonName14Manager.solve(segments)
        assertEquals(1, result.size)
        assertEquals(setOf(answer), result)
    }

    @Test
    fun canSolveDemonNameWithRepetitions() {
        val answer = "MorNixArgPhaDarHutRogNixKru"
        val segments = setOf(
            "Mor", "rNi", "Nix", "xAr", "Arg", "gPh", "Pha", "aDa", "Dar", "arH", "Hut", "utR",
            "Rog", "ogN", "gNi", "ixK", "Kru",
        )
        val result = DemonName14Manager.solve(segments)
        assertEquals(1, result.size)
        assertEquals(setOf(answer), result)
    }

    @Test
    fun doesntTryToSolveWithFewSegments() {
        val segments = setOf("Mor", "rNi", "Nix", "xAr", "Arg")
        assertTrue(DemonName14Manager.solve(segments).isEmpty())
    }

    @Test
    fun canSolveFake4CharSyllableExample() {
        val segments = setOf(
            "Hut", "utR", "tRo", "Rog", "ogN", "all", "llN", "lNi", "Nix", "ixA", "Arg", "rgP",
            "gPh", "Pha", "haD", "arH", "aDa", "ixK", "xKr",
        )
        val result = DemonName14Manager.solve(segments)
        assertEquals(1, result.size)
        assertTrue(result.contains("CallNixArgPhaDarHutRogNixKru"))
    }

    @Test
    fun canSolveRealExample_wRAR() {
        val segments = "Hut,utR,tRo,Rog,ogN,orN,rNi,Nix,ixA,Arg,rgP,gPh,Pha,haD,arH,aDa,ixK,xKr"
            .split(',').toSet()
        assertTrue(DemonName14Manager.solve(segments).contains("MorNixArgPhaDarHutRogNixKru"))
    }
}
