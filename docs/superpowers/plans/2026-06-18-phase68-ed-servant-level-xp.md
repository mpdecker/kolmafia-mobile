# Phase 68: Ed servant level/XP state + combat sync

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Continue Tier 1 after Phase 67 (`REVISION = "phase67"`, 1,921 tests). Close remaining Ed servant behavioral gap: persist per-servant name/level/XP from HTML, increment XP on combat wins, align `have_servant` with desktop `findEdServant`, and add minimal `$servant[field]` entity proxy reads.

**Status:** Complete (`GameRuntimeLibrary.REVISION = "phase68"`)

**Architecture:** Track A adds `EdServantRecord`/`EdServantState` pref storage and extends choice-1053/charpane parsers. Track B wires combat XP in `AdventureManager`, desktop-aligned `have_servant`, and richer `servants` CLI. Track C adds `ServantEntityFields` + `AshRuntime` SERVANT bracket indexing.

**Tech Stack:** Kotlin Multiplatform (`shared/commonMain` + `commonTest`), `EdServantManager`, `./gradlew.bat :shared:jvmTest`, `./gradlew.bat :androidApp:assembleDebug`, `GameRuntimeLibrary.REVISION = "phase68"`.

**Authority:** [`docs/parity-audit.md`](docs/parity-audit.md) Tier 1 #2 (Ed servant runtime depth).

---

## Track A — Per-servant state model

- [`servant/EdServantRecord.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/servant/EdServantRecord.kt)
- [`servant/EdServantState.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/servant/EdServantState.kt) — `_edServantRecords` pref encode/decode, XP cap 441, level-up at n²
- Extended [`EdServantChoiceSync`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/servant/EdServantChoiceSync.kt) and [`EdServantCharpaneSync`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/servant/EdServantCharpaneSync.kt)
- [`EdServantManager`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/servant/EdServantManager.kt) upserts records on sync

## Track B — Combat XP + have_servant + CLI

- `AdventureManager.resolveCombat` calls `edServantManager.addCombatExperience()` on win
- Crown of Ed hat → 2× XP delta
- `have_servant` requires stored `EdServantRecord` (no catalog-only fallback)
- `servants` CLI prints name/level/XP per summoned servant
- `EdServantManager` registered in `SharedModule` DI

## Track C — AshP32 servant entity fields

- [`ash/ServantEntityFields.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ServantEntityFields.kt)
- `GameRuntimeLibrary.resolveEntityIndex` + `AshRuntime` IndexNode for SERVANT bracket access
- Extended [`ServantData`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/modifiers/ServantData.kt) with level abilities

## Tests

- `EdServantStateTest`, `EdServantManagerTest`, `GameRuntimeLibraryAshP32Test`
- Updated `EdServantChoiceSyncTest`, `GameRuntimeLibraryAshP26Test`, `AshCompatibilityCorpusTest.corpus_edServantLevelXp_live`

## Closeout

| Item | Action |
|------|--------|
| `GameRuntimeLibrary.REVISION` | `"phase68"` |
| [`docs/parity-audit.md`](docs/parity-audit.md) | Tier 1 #2 Ed level/XP; Phase 68 history |
| Verify | `.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug` |

## Deferred (Phase 69+)

- Full desktop HTML `servants` table
- Entity proxy fields for THRALL / VYKEA / MONSTER
- Low-key tower adventure-key auto-fetch loop
