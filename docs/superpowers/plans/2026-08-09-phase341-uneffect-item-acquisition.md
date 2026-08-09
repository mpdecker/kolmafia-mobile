# Phase 341 — Uneffect item acquisition probes (TCRS applyModifiers v82)

## Context

Phase 340 completed mood `gain_effect` uneffect routing (`phase340`, TCRS v81). The highest-impact remaining uneffect deferral is desktop item-acquisition routing in `UneffectRequest.getAction()`.

## Delivered

1. **`UneffectRemovableMaps.needsCocoa`** + public `HOT_DREADSYLVANIAN_COCOA` constant
2. **`UneffectItemAcquisition`** — NPC/coinmaster/mall/stash acquisition probes with `(needsCocoa || !hasRemedy)` mall/stash gate
3. **`UneffectActionContext.canAcquireUneffectItem`** + resolver `UseItem(retrieveFirst=true)` branch
4. **`GameRuntimeLibrary.uneffectByName`** — `buildCheckContext` wiring + needsCocoa error before HTTP shrug
5. **Tests** — `UneffectRemovableMapsTest`, `UneffectActionResolverTest`, `UneffectItemAcquisitionTest`

## Deferred (Phase 342+)

| Item | Reason |
|------|--------|
| `lose_effect` → `EffectDatabase.getDefaultAction` + `getKnownSources` | Mood lose_effect execution path |
| Desktop `username_moods.txt` file import | Separate persistence track |
| `removeEffectsWithSkill` local effect stack mutation | No full desktop effect stack |
| Shruggable/timer/Asdon HTTP uneffect run() branches | Separate uneffect run() depth |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.request.UneffectRemovableMapsTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.request.UneffectActionResolverTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.request.UneffectItemAcquisitionTest"
```
