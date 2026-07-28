# Phase 141: AshP99 post-bird learnSkill + fetchSkills

**Date:** 2026-07-22  
**Revision:** `phase141`  
**Tests:** 2,654

## Goal

When bird-of-day desc sync first unlocks Seek-a-Bird (`_canSeekBirds` false→true), locally learn skill 7323 and refresh `SkillManager` from api.php so `have_skill` and `mp_cost` work immediately after login desc checks.

## Delivered

### SkillLearner + SkillManager

- [`SkillLearner.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/skill/SkillLearner.kt) — desktop-style `learnSkill` (skillLevel pref + merge into manager state)
- [`SkillManager.learnLocalSkill`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/skill/SkillManager.kt) — merge/replace skill by id in cached state

### BirdOfTheDaySync + visit hook

- [`BirdOfTheDaySync.applySeekBirdSkillDescription`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/BirdOfTheDaySync.kt) — returns `newlyUnlocked` when `_canSeekBirds` transitions false→true; `SEEK_OUT_A_BIRD_BASE_NAME` constant
- [`GameRuntimeLibrary.processVisitResponseHooks`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt) — on newly unlocked bird skill: `SkillLearner.learnSkill` + `fetchSkills()`

### Tests

- [`SkillLearnerTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/skill/SkillLearnerTest.kt)
- Extended [`BirdOfTheDaySyncTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/quest/BirdOfTheDaySyncTest.kt) — unlock return value cases
- [`GameRuntimeLibraryAshP99Test.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryAshP99Test.kt) — bird desc visit → `have_skill` smoke

## Deferred (Phase 142+)

- Codpiece equip HTTP (`inventory.php?action=docodpiece`)
- Rewire all `available_amount` retrieve semantics
- Full async storage/stash fetch in `buildCheckContext()`
- General battle `learnSkillFromResponse` item-consumption cases

## Notes

- Local learn uses base name from `SkillDefinitionDatabase`; `fetchSkills()` replaces with daily variant (e.g. `"Seek out a Turkey"`)
- `fetchSkills()` is best-effort; local learn still makes `have_skill` work if API fetch fails
