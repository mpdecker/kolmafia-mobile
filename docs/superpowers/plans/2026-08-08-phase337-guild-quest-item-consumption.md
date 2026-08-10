# Phase 337 — Guild quest item consumption (TCRS applyModifiers v78)

## Context

Phase 336 completed AoL uneffect maps (`phase336`, TCRS v77). The highest-impact **behavioral** gap from the Phase 336 deferred list is guild quest **item consumption** — quest step advancement already works via `QuestAdvanceRules.kt` and `QuestLogSync.processResponse`, but turn-in items are not deducted locally.

Desktop reference: `GuildRequest.handleGuildQuests`

| Place | Trigger HTML | Item consumed | Quest |
|-------|-------------|---------------|-------|
| `ocg` | `"hand over Fernswarthy's key"` / `"returned with Fernswarthy's key"` / `"takes Fernswarthy's key"` | Fernswarthy's key (2277) | EGO → step1 |
| `challenge` | `"Eleven inches"` | big knob sausage (5193) | MUSCLE → finished |
| `challenge` | `"captured poltersandwich"` | exorcised sandwich (5194) | MYST → finished |

## Delivered

1. **`QuestLogSync.applyEgoKeyTurnIn` v2** — `place == "ocg"`, HTML gating, key consumption at step1
2. **`QuestLogSync.applyGuildChallengeTurnIn`** — sausage/sandwich turn-ins at `challenge` with HTML gating + consume
3. **`applyPlaceHooks`** — passes `responseText` html; dispatches ocg/challenge turn-in helpers
4. **`GuildQuestSync.applyPlaceVisit`** — dispatches paco/ocg/challenge (paco unchanged)
5. **Tests** — `QuestLogSyncTest` + `GuildQuestSyncTest` coverage for turn-in consumption and negative cases

## Deferred (Phase 338+)

| Item | Reason |
|------|--------|
| Hot Tub / item-remedy uneffect branches | Uneffect-depth phase |
| `removeEffectsWithSkill` local effect removal | No `UseSkillRequest` on mobile |
| `DreadScrollManager.decorate()` | Choice-UI HTML; headless non-goal |
| Full `scg` nemesis text hooks beyond existing rules | Mostly covered by `QuestAdvanceRules` |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.session.GuildQuestSyncTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.quest.QuestLogSyncTest"
```
