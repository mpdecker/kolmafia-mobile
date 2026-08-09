# Phase 338 — Uneffect Hot Tub + item/remedy routing (TCRS applyModifiers v79)

## Context

Phase 337 completed guild quest item consumption (`phase337`, TCRS v78). The highest-impact **behavioral** gap from the Phase 336–337 deferred list is uneffect routing depth.

Desktop `UneffectRequest.getAction()` adds, after skill lookup:
- Hot tub for Shake It Off removables when VIP key + soak budget + prefs allow
- Item use for mapped removables (antidote, tiny house, cocoa, etc.) when item is accessible in inventory
- Remedy / ancient cure-all retrieve+use before generic HTTP shrug

## Delivered

1. **`UneffectRemovableMaps`** — `getUneffectItemId`, `removableByShakeItOff`, `REMEDY`/`ANCIENT_CURE_ALL` constants
2. **`UneffectActionResolver`** — skill → hot tub → mapped item → cure-all/remedy → HTTP routing with desktop hot tub gates
3. **`GameRuntimeLibrary.uneffectByName`** — executes resolver actions; `hottub`/`soak` CLI
4. **`ClanLoungeRequest.useHotTub`** — POST `action=hottub`; `ClanLoungeSync.syncHotTubSoaksFromHtml` → `_hotTubSoaks`
5. **Tests** — `UneffectActionResolverTest`, extended `UneffectRemovableMapsTest`, `ClanLoungeRequestTest`, `GameRuntimeLibraryCliTest`

## Deferred (Phase 339+)

| Item | Reason |
|------|--------|
| Mood `gain_effect` pre-defined action routing | MoodManager depth |
| Mall/clan stash auto-buy for uneffect items | Purchase probe wiring |
| `removeEffectsWithSkill` local active-effect mutation | No full desktop effect stack |
| `needsCocoa` purchase/coinmaster branch | Cocoa acquisition complexity |
| Guild EGO step2/finish dusty-book consumption | Quest track |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.request.UneffectRemovableMapsTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.request.UneffectActionResolverTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.request.ClanLoungeRequestTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.ash.GameRuntimeLibraryCliTest.cliExecute_uneffect_callsUneffectRequest"
```
