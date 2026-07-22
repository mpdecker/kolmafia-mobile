# Phase 130: AshP88 COMBAT_SKILL consequence sync

**Date:** 2026-06-24  
**Revision:** `phase130`  
**Tests:** 2,550

## Goal

Wire the fourteen `COMBAT_SKILL` consequence rows from `consequences.txt` so in-combat fight HTML with a `<select name=whichskill>` dropdown syncs expression/capture prefs (Force uses, Powerful Glove battery, Heartstone letters, Back-Up path-aware uses, etc.).

## Delivered

### Consequence wiring

- [`CombatSkillConsequenceDatabase.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/CombatSkillConsequenceDatabase.kt) — loads `COMBAT_SKILL` rows indexed by skill id
- [`CombatSkillDropdownParser.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/CombatSkillDropdownParser.kt) — desktop `AVAILABLE_COMBATSKILL` regex; guards on `whichskill` select and fight-not-won
- [`CombatSkillConsequenceSync.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/CombatSkillConsequenceSync.kt) — applies rules against dropdown labels
- Fight visit hook in [`GameRuntimeLibrary.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt) when `fight.php` / `You're fighting`
- Wired via [`GameDatabase.load()`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/GameDatabase.kt) after `SkillDefinitionDatabase.load()`

### ConsequenceActionResolver / ExpressionContext

- [`ConsequenceActionResolver.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/ConsequenceActionResolver.kt) — `Context.expressionContext` threaded into bracket evaluation for path-aware rules
- [`ModifierExpression.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/modifiers/ModifierExpression.kt) — `path()` reads comma-containing path names until `)` (desktop `until(")")` parity for `path(You, Robot)`)

### Tests

- `CombatSkillConsequenceDatabaseTest`, `CombatSkillDropdownParserTest`, `CombatSkillConsequenceSyncTest`
- Extended `ConsequenceActionResolverTest`, `ModifierExpressionTest`

## Deferred (Phase 131+)

- MONSTER disambiguation rows (7 hard-mode name replacements)
- Incomplete DESC_ITEM rows (Kremlin's Greatest Briefcase, Baseball Diamond)
- Full `parseAvailableCombatSkills` side effects (availableCombatSkillsList, lovebug/gladiator unlock prefs)
- Desktop `updateOneDesc()` rotation prefetch
- Full `InventoryManager.checkMods()` inventory gating

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
