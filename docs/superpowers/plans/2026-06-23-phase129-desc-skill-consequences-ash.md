# Phase 129: AshP87 DESC_SKILL consequence sync

**Date:** 2026-06-23  
**Revision:** `phase129`  
**Tests:** 2,538

## Goal

Wire the eight `DESC_SKILL` consequence rows from `consequences.txt` so self-skill description visits (`desc_skill.php?whichskill=&self=true`) sync expression/capture prefs via the existing `ConsequenceActionResolver` pipeline.

## Delivered

### SkillDefinitionDatabase

- [`SkillDefinitionDatabase.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/SkillDefinitionDatabase.kt) — `registerForTest()` / `resetForTest()`

### Consequence wiring

- [`SkillDescriptionConsequenceDatabase.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/SkillDescriptionConsequenceDatabase.kt) — loads `DESC_SKILL` rows indexed by skill id
- [`SkillDescriptionConsequenceSync.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/SkillDescriptionConsequenceSync.kt) — applies rules on desc_skill visits
- `desc_skill.php?whichskill=` visit hook gated on `self=true` in [`GameRuntimeLibrary.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt)
- Wired via [`GameDatabase.load()`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/GameDatabase.kt) after `SkillDefinitionDatabase.load()`

### ConsequenceActionResolver fix

- [`ConsequenceActionResolver.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/ConsequenceActionResolver.kt) — `SetLiteral` now runs `substituteGroups()` (desktop parity for pipe-separated captures like `banishingShoutMonsters=$1|$2|$3`)

### Tests

- `SkillDefinitionDatabaseTest`, `SkillDescriptionConsequenceDatabaseTest`, `SkillDescriptionConsequenceSyncTest`

## Deferred (Phase 130+)

- COMBAT_SKILL rows (fight dropdown HTML)
- MONSTER disambiguation rows
- Incomplete DESC_ITEM rows (Kremlin's Greatest Briefcase, Baseball Diamond)
- Desktop `updateOneDesc()` rotation prefetch
- Full `InventoryManager.checkMods()` inventory gating

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
