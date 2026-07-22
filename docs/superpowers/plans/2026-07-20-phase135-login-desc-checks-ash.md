# Phase 135: AshP93 login desc checks v3

**Date:** 2026-07-20  
**Revision:** `phase135`  
**Tests:** 2,608

## Goal

Close Phase 134 deferrals for login-time desc prefetch: crown/bjorn familiar-item descs, bird-of-day skill+effects sync, Entauntauned/Savage Beast effect descs, and closet-inclusive KGB/Baseball owned checks.

## Delivered

### BirdOfTheDaySync

- [`BirdOfTheDaySync.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/BirdOfTheDaySync.kt) — `checkBirdOfTheDay()`, `applySeekBirdSkillDescription()`, skill name/MP parsers, `_birdOfTheDay` / `_canSeekBirds` / `_birdsSoughtToday` prefs
- [`DynamicItemModifierSync.DescVisit.Skill`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/DynamicItemModifierSync.kt) for `desc_skill.php?whichskill=7323&self=true`
- [`GameRuntimeLibrary.processVisitResponseHooks`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt) — bird skill hook after `SkillDescriptionConsequenceSync`

### Login desc checks

- [`DynamicItemModifierSync.checkLoginDescChecks()`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/DynamicItemModifierSync.kt) — crown, bjorn, bird, Entauntauned (always), Savage Beast (active, always visit)
- `_savageBeastMods` moved to extended/login handling (removed from v1 pref-gated loop)
- [`checkOwnedItemDescriptions`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/DynamicItemModifierSync.kt) uses `isAccessible()` for closet-inclusive KGB/Baseball

### Tests

- [`BirdOfTheDaySyncTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/quest/BirdOfTheDaySyncTest.kt)
- Extended [`DynamicItemModifierSyncTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/quest/DynamicItemModifierSyncTest.kt) login/owned cases

## Deferred (Phase 136+)

- `checkSkillGrantingEquipment` + conditional skill inventory/equipped modifiers
- `checkCoatOfPaint(playerClassChanged)` class-change refresh hook
- Full desktop `itemAvailable` (mall/storage/stash/coinmaster)
- Explicit `learnSkill` / post-bird `skillManager.fetchSkills()` refresh
- Familiar-id parsing from crown/bjorn item desc HTML

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
