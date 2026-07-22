# Phase 126: AshP84 DESC_ITEM consequence expressions

**Date:** 2026-06-20  
**Revision:** `phase126`  
**Tests:** 2,498

## Goal

Wire DESC_ITEM rules from bundled `consequences.txt` to item-description HTML visits, extending the Phase 122 consequence resolver with `roman()` and `monstername` support.

## Delivered

### Shared consequence types

- [`ConsequenceTypes.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/ConsequenceTypes.kt) — shared `ConsequenceRule` / `ConsequenceAction` + `ConsequenceActionParser`
- Refactored [`QuestLogConsequenceDatabase.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/QuestLogConsequenceDatabase.kt) to use shared types (QUEST_LOG behavior unchanged)

### DESC_ITEM loader + sync

- [`ItemDescriptionConsequenceDatabase.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/ItemDescriptionConsequenceDatabase.kt) — parses DESC_ITEM rows, indexes by `ItemData.descId`; skips empty-regex and `=mods` rules
- [`ItemDescriptionConsequenceSync.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/ItemDescriptionConsequenceSync.kt) — applies rules on `desc_item.php` HTML
- [`ItemDatabase.getByDescId`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/ItemDatabase.kt) — descId lookup index
- Wired via [`GameDatabase.load()`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/GameDatabase.kt)

### Resolver extensions

- [`ConsequenceActionResolver.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/ConsequenceActionResolver.kt) — shared `fireAction()`; `monstername` literal resolution
- [`RomanNumerals.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/util/RomanNumerals.kt) + `roman()` in [`ModifierExpression.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/modifiers/ModifierExpression.kt)
- [`GameRuntimeLibrary.processVisitResponseHooks`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt) — `desc_item.php?whichitem=` hook

### Tests

- `ItemDescriptionConsequenceSyncTest` + extended `ConsequenceActionResolverTest` + `RomanNumeralsTest`

## Deferred (Phase 127+)

- `=mods` DESC_ITEM rules (item enchantment parsing + `ModifierDatabase.overrideModifier`)
- Incomplete DESC_ITEM rows (Kremlin's Greatest Briefcase, Baseball Diamond)
- DESC_SKILL / DESC_EFFECT / COMBAT_SKILL / MONSTER consequence types
- Desktop `updateOneDesc()` rotation prefetch

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
