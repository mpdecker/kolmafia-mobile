# Phase 128: AshP86 DESC_EFFECT consequence sync

**Date:** 2026-06-22  
**Revision:** `phase128`  
**Tests:** 2,530

## Goal

Extend the Phase 126–127 consequence pipeline to `DESC_EFFECT` rows in `consequences.txt`: expression-based pref capture plus eleven `=mods` effect enchantment rules, wired to `desc_effect.php` visits with Effect modifier overrides and cached pref restore.

## Delivered

### EffectDatabase

- [`EffectDatabase.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/EffectDatabase.kt) — `byDescId` index, `getByDescId()`, `registerForTest()` / `resetForTest()`

### Enchantment parsing

- [`StandardEnchantmentParser.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/StandardEnchantmentParser.kt) — shared desktop `parseStandardEnchantments` loop extracted from item parser
- [`EffectEnchantmentParser.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/EffectEnchantmentParser.kt) — desktop `DebugDatabase.parseEffectEnchantments` using `<font color=blue><b>…</b></font>` pattern
- [`ItemEnchantmentParser.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/ItemEnchantmentParser.kt) — refactored to reuse `StandardEnchantmentParser`

### Consequence wiring

- [`EffectDescriptionConsequenceDatabase.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/EffectDescriptionConsequenceDatabase.kt) — loads `DESC_EFFECT` rows from `consequences.txt`
- [`EffectDescriptionConsequenceSync.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/EffectDescriptionConsequenceSync.kt) — applies rules on desc_effect visits
- `ConsequenceAction.SetEffectMods` + [`ConsequenceActionResolver`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/ConsequenceActionResolver.kt) `effectSpec` parameter
- `desc_effect.php?whicheffect=` visit hook in [`GameRuntimeLibrary.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibrary.kt)
- Wired via [`GameDatabase.load()`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/GameDatabase.kt) after `EffectDatabase.load()`

### Session restore

- [`DynamicItemModifierSync.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/DynamicItemModifierSync.kt) — nine effect pref→name mappings restored alongside item prefs

### Tests

- `EffectDatabaseTest`, `EffectDescriptionConsequenceDatabaseTest`, `EffectDescriptionConsequenceSyncTest`, `EffectEnchantmentParserTest`, extended `DynamicItemModifierSyncTest`

## Deferred (Phase 129+)

- DESC_SKILL / COMBAT_SKILL / MONSTER consequence types
- Incomplete DESC_ITEM rows (Kremlin's Greatest Briefcase, Baseball Diamond)
- Desktop `updateOneDesc()` rotation prefetch
- Full `InventoryManager.checkMods()` inventory gating
- EffectDatabase disambiguation by descid (desktop `getEffectIdFromDescription`)

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
