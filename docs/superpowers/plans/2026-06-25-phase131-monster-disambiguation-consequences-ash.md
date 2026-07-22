# Phase 131: AshP89 MONSTER disambiguation consequences

**Date:** 2026-06-25  
**Revision:** `phase131`  
**Tests:** 2,560

## Goal

Wire the eight `MONSTER` disambiguation rows from `consequences.txt` so combat monster resolution renames ambiguous bosses (Ed forms, Hard Mode variants) from fight/adventure HTML — completing the consequences.txt pipeline except incomplete DESC_ITEM rows.

## Delivered

### Action type

- [`ConsequenceTypes.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/ConsequenceTypes.kt) — `ConsequenceAction.ReturnReplacement` + quoted-action parsing in `ConsequenceActionParser`
- [`ConsequenceActionResolver.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/ConsequenceActionResolver.kt) — `resolveReplacement()` for group substitution on quoted templates

### Consequence wiring

- [`MonsterConsequenceDatabase.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/MonsterConsequenceDatabase.kt) — loads `MONSTER` rows indexed by monster name (file-order rule lists)
- [`MonsterConsequenceSync.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/MonsterConsequenceSync.kt) — `disambiguateMonster(name, html)` with first-match rule semantics
- Wired via [`GameDatabase.load()`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/GameDatabase.kt) after `MonsterDatabase.load()`

### Combat hooks

- [`RandomModifierParser.resolveTemplate`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/combat/RandomModifierParser.kt) — disambiguates after MONSTERID lookup and before name lookup
- [`AdventureManager.prepareCombatMonster`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/adventure/AdventureManager.kt) — disambiguates stripped name before template resolution; `_lastMonster` pref uses disambiguated definition

### Tests

- `MonsterConsequenceDatabaseTest`, `MonsterConsequenceSyncTest`
- Extended `RandomModifierParserTest`, `ConsequenceActionResolverTest`

## Deferred (Phase 132+)

- Incomplete DESC_ITEM rows (Kremlin's Greatest Briefcase, Baseball Diamond)
- Desktop `updateOneDesc()` rotation prefetch
- Full `InventoryManager.checkMods()` inventory gating
- Dynamic monster registration (desktop `registerMonster`)

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
