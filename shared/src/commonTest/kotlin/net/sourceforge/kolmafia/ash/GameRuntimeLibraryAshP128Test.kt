package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP128Test {

    @Test
    fun canExpandStomach_falseInRobocore() {
        val character = net.sourceforge.kolmafia.character.KoLCharacter()
        character.updateFromApiResponse(
            net.sourceforge.kolmafia.character.CharacterApiResponse(
                name = "t",
                playerid = "1",
                level = "1",
                classId = "1",
                sign = "",
                path = "You, Robot",
                ascensions = "1",
                gender = "m",
                title = "",
                hp = "1",
                hpmax = "1",
                basehpmax = "1",
                mp = "1",
                mpmax = "1",
                basempmax = "1",
                mus = "1",
                musexp = "0",
                mys = "1",
                mysexp = "0",
                mox = "1",
                moxexp = "0",
                buffedmus = "1",
                buffedmys = "1",
                buffedmox = "1",
                meat = "0",
                storagemeat = "0",
                adventures = "0",
                turnsplayed = "0",
                currentrun = "0",
                daycount = "0",
                rollover = "0",
                fullness = "0",
                drunk = "0",
                spleen = "0",
                stomachsize = "0",
                liversize = "0",
                spleensize = "0",
                pvpfights = "0",
                hippystone = "0",
                roninleft = "0",
                hardcore = "0",
                kingliberated = "0",
                limitmode = "",
                stills = "-1",
            ),
        )
        val lib = GameRuntimeLibrary(character = character)
        assertEquals("false", outputLib(lib, """print(can_expand_stomach());""").trim())
        assertEquals("false", outputLib(lib, """print(can_expand_liver());""").trim())
    }

    @Test
    fun canExpandStomach_trueForStandard() {
        val lib = GameRuntimeLibrary(
            character = net.sourceforge.kolmafia.character.KoLCharacter().also {
                it.updateFromApiResponse(
                    net.sourceforge.kolmafia.character.CharacterApiResponse(
                        name = "t",
                        playerid = "1",
                        level = "1",
                        classId = "1",
                        sign = "",
                        path = "Standard",
                        ascensions = "1",
                        gender = "m",
                        title = "",
                        hp = "1",
                        hpmax = "1",
                        basehpmax = "1",
                        mp = "1",
                        mpmax = "1",
                        basempmax = "1",
                        mus = "1",
                        musexp = "0",
                        mys = "1",
                        mysexp = "0",
                        mox = "1",
                        moxexp = "0",
                        buffedmus = "1",
                        buffedmys = "1",
                        buffedmox = "1",
                        meat = "0",
                        storagemeat = "0",
                        adventures = "0",
                        turnsplayed = "0",
                        currentrun = "0",
                        daycount = "0",
                        rollover = "0",
                        fullness = "0",
                        drunk = "0",
                        spleen = "0",
                        stomachsize = "15",
                        liversize = "14",
                        spleensize = "15",
                        pvpfights = "0",
                        hippystone = "0",
                        roninleft = "0",
                        hardcore = "0",
                        kingliberated = "0",
                        limitmode = "",
                        stills = "-1",
                    ),
                )
            },
        )
        assertEquals("true", outputLib(lib, """print(can_expand_stomach());""").trim())
        assertEquals("true", outputLib(lib, """print(can_expand_liver());""").trim())
    }

    @Test
    fun revision_isphase170() {
        assertEquals("phase450", GameRuntimeLibrary.REVISION)
    }
}
