# Phase 115: AshP73 Intergnat demon name sync

**Date:** 2026-06-19  
**Track:** Tier 1 ASH behavioral parity  
**REVISION:** `phase115`

## Goal

Port desktop Intergnat eldritch-fight demon name/contact sync from fight HTML into mobile combat processing, persisting `demonName12` for summoning-chamber parity.

## Deliverables

| Area | Change |
|------|--------|
| `IntergnatDemonNameSync.kt` | 4 fight regexes + `updateIntergnatName` state machine |
| `Preferences.DEMON_NAME_12` | Desktop `demonName12` key |
| `AdventureManager.resolveCombat` | Hook after fight HTML when Intergnat + `eldritch` modifier |
| REVISION | `phase115` |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred (Phase 116+)

- Equipment beeosity for Maximizer / BHY gear limits
- Summoning chamber demon summon HTTP
- AshP8–P18 remaining stubs / Maximizer Evaluator
