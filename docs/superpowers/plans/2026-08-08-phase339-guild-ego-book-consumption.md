# Phase 339 — Guild EGO dusty-book finish consumption (TCRS applyModifiers v80)

## Context

Phase 338 completed uneffect hot tub + item/remedy routing (`phase338`, TCRS v79). The highest-impact **behavioral** gap from the Phase 338 deferrals is **Guild EGO step6/finish dusty-book consumption**.

Desktop `GuildRequest.handleGuildQuests` + `ResultProcessor` consume dusty old book (2279) and Fernswarthy's key (2277) when the guild manual reward is received.

## Delivered

1. **`QuestLogSync` constants** — `DUSTY_BOOK_ID`, `MUS/MYS/MOX_MANUAL_ID`
2. **`QuestLogSync.applyEgoBookTurnIn`** — `ocg` + step6/finished + manual HTML gates; consumes book + key
3. **`QuestLogSync.consumeEgoBookTurnInItems`** — shared idempotent consume helper
4. **Wiring** — `applyPlaceHooks` + `GuildQuestSync` ocg branch
5. **`QuestAdvanceRules`** — Manual of Labor/Transmission FINISHED rules
6. **`QuestItemRules.applyItemsGained`** — manual acquisition consume via optional callbacks; `AdventureManager` wired
7. **Tests** — `GuildQuestSyncTest`, `QuestLogSyncTest`, `QuestItemRulesTest`

## Deferred (Phase 340+)

| Item | Reason |
|------|--------|
| Mood `gain_effect` pre-defined uneffect routing | MoodManager depth |
| Uneffect mall/clan stash auto-buy (`needsCocoa`) | Purchase probe wiring |
| `removeEffectsWithSkill` local effect stack mutation | No full desktop effect stack |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.session.GuildQuestSyncTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.quest.QuestLogSyncTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.quest.QuestItemRulesTest"
```
