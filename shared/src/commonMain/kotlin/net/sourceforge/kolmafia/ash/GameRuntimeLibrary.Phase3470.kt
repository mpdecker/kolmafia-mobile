package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.session.DvorakManager
import net.sourceforge.kolmafia.session.MailManager
import net.sourceforge.kolmafia.session.NemesisManager

/** Phases 3411–3470: Nemesis, Tavern/Dvorak, and mail/spading session glue. */
internal fun GameRuntimeLibrary.registerPhase3470(scope: AshScope) {
    regFn(scope, "nemesis_password", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.let(NemesisManager::password).orEmpty())
    }
    regFn(scope, "dvorak_status", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(DvorakManager.status())
    }
    regFn(scope, "dvorak_next_step", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(DvorakManager.nextStepUrl().orEmpty())
    }
    regFn(scope, "mail_has_new_messages", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(preferences?.let(MailManager::hasNewMessages) ?: false)
    }
    regFn(scope, "mail_count", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(MailManager.messages().size.toLong())
    }
    regFn(scope, "spading_enabled", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(net.sourceforge.kolmafia.session.SpadingManager.enabled(preferences))
    }
    regFn(scope, "spading_last_event", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(preferences?.getString("_lastSpadingEvent", "").orEmpty())
    }
}
