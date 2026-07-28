# Phase 144: AshP102 Battle Skill Learn Parity

**Date:** 2026-07-27  
**Revision:** `phase144`  
**Tests:** 2,680 (full suite)

## Goal

Port desktop `ResponseTextParser.learnSkillFromResponse`: parse fight HTML for newly learned skills, consume triggering items (tattered standards, dictionary, sheet music), update leveled `skillLevel*` prefs, and merge into `SkillManager` so `have_skill` works immediately after combat.

## Delivered

### SkillLearnFromResponse

- [`SkillLearnFromResponse.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/session/SkillLearnFromResponse.kt) — NEWSKILL1 (name) / NEWSKILL3 (id) parse
- [`BattleLearnSkillIds.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/skill/BattleLearnSkillIds.kt) — skill/item id constants

### SkillLearner extensions

- [`SkillLearner.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/skill/SkillLearner.kt) — item consumption, uncapped/capped pref increment, `firstLearnOnly` for bird desc unlock
- [`SkillMaxLevel.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/SkillMaxLevel.kt) — Slimy/Chitinous max levels

### Hooks

- [`AdventureManager.resolveCombat`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt)
- [`GameRuntimeLibrary.processVisitResponseHooks`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt) fight block

### Tests

- [`SkillLearnFromResponseTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/session/SkillLearnFromResponseTest.kt)
- Extended [`SkillLearnerTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/skill/SkillLearnerTest.kt)
- Extended [`AdventureManagerTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/adventure/AdventureManagerTest.kt)
- [`GameRuntimeLibraryAshP102Test.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryAshP102Test.kt)

## Deferred (Phase 145+)

- Deeper `AccessibleItemCount` / familiar-equipped / Hat Trick / path gates
- Expand `AshCompatibilityCorpusTest` assertions
- Desktop learnSkill side effects (bookshelf, GreyYou, DiscoCombatHelper)
