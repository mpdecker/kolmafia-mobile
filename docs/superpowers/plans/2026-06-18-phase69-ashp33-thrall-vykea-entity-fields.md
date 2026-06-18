# Phase 69: AshP33 THRALL + VYKEA entity bracket fields

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Continue Tier 1 after Phase 68 (`REVISION = "phase68"`, 1,934 tests). Extend AshP32 `resolveEntityIndex` to THRALL and VYKEA bracket field reads and expand the ASH compatibility corpus.

**Status:** Complete (`GameRuntimeLibrary.REVISION = "phase69"`)

**Architecture:** Track A adds `ThrallEntityFields` (PastaThrall prefs + ModifierDatabase). Track B adds `VykeaEntityFields` (VykeaCompanionData catalog). Track C adds AshP33 tests + `corpus_thrallVykeaEntityFields_live`.

**Tech Stack:** Kotlin Multiplatform (`shared/commonMain` + `commonTest`), `./gradlew.bat :shared:jvmTest`, `./gradlew.bat :androidApp:assembleDebug`, `GameRuntimeLibrary.REVISION = "phase69"`.

**Authority:** [`docs/parity-audit.md`](docs/parity-audit.md) Tier 1 #1 (ASH behavioral / expand corpus).

---

## Track A — THRALL entity bracket fields

- [`ash/ThrallEntityFields.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/ThrallEntityFields.kt): `id`, `name`, `level`, `skill`, `current_modifiers`, `image`/`tinyimage`
- Wired in `GameRuntimeLibrary.resolveEntityIndex` for `AshType.THRALL`

## Track B — VYKEA entity bracket fields

- [`ash/VykeaEntityFields.kt`](shared/src/commonMain/kotlin/net/sourceforge/kolmafia/ash/VykeaEntityFields.kt): `id`, `name`, `type`, `level`, `modifiers`, `rune`, `image`, `attack_element`
- Wired in `GameRuntimeLibrary.resolveEntityIndex` for `AshType.VYKEA`

## Track C — Tests + corpus

- [`GameRuntimeLibraryAshP33Test.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/GameRuntimeLibraryAshP33Test.kt)
- [`AshCompatibilityCorpusTest.kt`](shared/src/commonTest/kotlin/net/sourceforge/kolmafia/ash/AshCompatibilityCorpusTest.kt): `corpus_thrallVykeaEntityFields_live`

## Closeout

| Item | Action |
|------|--------|
| `GameRuntimeLibrary.REVISION` | `"phase69"` |
| [`docs/parity-audit.md`](docs/parity-audit.md) | Tier 1 #1 AshP33; Phase 69 history |
| Verify | `.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug` |

## Deferred (Phase 70+)

- MONSTER entity bracket fields
- VYKEA charpane sync for live `my_vykea_companion`
- Full desktop HTML `servants` table
- `journeyman.txt` loader
- Low-key tower adventure-key auto-fetch
