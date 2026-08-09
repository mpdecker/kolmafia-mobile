package net.sourceforge.kolmafia.mood

import kotlin.test.Test
import kotlin.test.assertFalse

class EffectGainGateTest {

    @Test fun cannotGainEffect_stubAlwaysFalse() {
        assertFalse(EffectGainGate.cannotGainEffect(1))
        assertFalse(EffectGainGate.cannotGainEffect(999))
    }
}
