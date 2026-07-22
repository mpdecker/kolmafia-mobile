# Phase 127: AshP85 DESC_ITEM `=mods` enchantment sync

**Date:** 2026-06-21  
**Revision:** `phase127`  
**Tests:** 2,516

## Goal

Wire the nine deferred `=mods` DESC_ITEM consequence rules so desc_item visits parse blue-font enchantments, override live Item modifiers, and persist pref strings — matching desktop `ConsequenceManager` + `InventoryManager.checkItem`.

## Delivered

### Enchantment parsing

- [`ModifierEnchantmentParser.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/modifiers/ModifierEnchantmentParser.kt) — desktop `ModifierDatabase.parseModifier` + enum desc patterns + auxiliary HTML extractors
- [`ItemEnchantmentParser.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/ItemEnchantmentParser.kt) — desktop `DebugDatabase.parseItemEnchantments` / `parseStandardEnchantments`
- `parseModifierFromDesc()` delegates on `DoubleModifier`, `BooleanModifier`, `StringModifier`, `BitmapModifier`

### Modifier override

- [`ModifierDatabase.overrideModifier`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/data/ModifierDatabase.kt) with desktop CARRIED_OVER merge from bundled `modifiers.txt` base entries
- `resetForTest()` / `resetOverridesForTest()` for test isolation

### Consequence wiring

- `ConsequenceAction.SetItemMods` + loader accepts empty-regex `=mods` rows
- [`ConsequenceActionResolver`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/ConsequenceActionResolver.kt) parses HTML, overrides Item, sets pref
- [`ItemDescriptionConsequenceSync`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/ItemDescriptionConsequenceSync.kt) passes `rule.spec` + full HTML

### Session restore

- [`DynamicItemModifierSync`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/quest/DynamicItemModifierSync.kt) — 8 pref→item mappings; hooked from `GameRuntimeLibrary` init

### Tests

- `ModifierEnchantmentParserTest`, `ItemEnchantmentParserTest`, extended `ItemDescriptionConsequenceSyncTest`, `DynamicItemModifierSyncTest`

## Deferred (Phase 128+)

- DESC_EFFECT `=mods` rows
- Incomplete DESC_ITEM rows (Kremlin's Greatest Briefcase, Baseball Diamond)
- Desktop `updateOneDesc()` rotation prefetch
- Full `InventoryManager.checkMods()` inventory gating

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
