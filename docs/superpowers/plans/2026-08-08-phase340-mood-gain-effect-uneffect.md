# Phase 340 — Mood gain_effect uneffect routing (TCRS applyModifiers v81)

## Context

Phase 339 completed guild EGO dusty-book consumption (`phase339`, TCRS v80). The highest-impact **behavioral** gap from the Phase 339 deferrals is **mood `gain_effect` predefined uneffect routing**.

Desktop `UneffectRequest.getAction()` checks `MoodManager.getDefaultAction("gain_effect", name)` before skill/hottub/item/remedy routing.

## Delivered

1. **`MoodRemovalTrigger` + `MoodRemovalTriggerParser`** — desktop `gain_effect`/`lose_effect`/`unconditional` line parsing
2. **`Mood.removalTriggers` + `effectiveRemovalTriggers`** — parent mood merge with child override by type+effectName
3. **`MoodManager`** — `moodRemovalTriggers_*` pref persistence, `addRemovalTrigger`, `getDefaultAction(gain_effect)`
4. **`UneffectRemovableMaps.isRemovable`** — desktop blacklist with default removable
5. **`MoodUneffectActionParser`** — parse mood `cast`/`use`/`hottub` actions into `UneffectAction`
6. **`UneffectActionResolver`** — mood predefined action first step + cast skill-ownership gate
7. **`GameRuntimeLibrary.uneffectByName`** — wires `moodManager.getDefaultAction("gain_effect", …)`
8. **Tests** — `MoodRemovalTriggerParserTest`, `MoodManagerDefaultActionTest`, extended `UneffectActionResolverTest`

## Deferred (Phase 341+)

| Item | Reason |
|------|--------|
| Desktop `username_moods.txt` file import | Separate persistence track |
| `lose_effect` → `EffectDatabase` default + knownSources | Mood lose_effect execution path |
| Uneffect mall/clan stash auto-buy (`needsCocoa`) | Purchase probe wiring in resolver |
| `removeEffectsWithSkill` local effect stack mutation | No full desktop effect stack |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodRemovalTriggerParserTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodManagerDefaultActionTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.request.UneffectActionResolverTest"
```
