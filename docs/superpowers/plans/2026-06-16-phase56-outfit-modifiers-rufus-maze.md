# Phase 56: Outfit Modifiers, Rufus Shadow Labyrinth, CLI maze

**Goal:** Close Tier 1 outfit modifier gap, deferred Rufus Shadow Labyrinth automation, and v1 `maze` CLI hedge-maze runner.

**Status:** Complete (`GameRuntimeLibrary.REVISION = "phase56"`)

---

## Track A — AshP21 type:name modifiers

| File | Change |
| ---- | ------ |
| `GameRuntimeLibrary.AshP21Batch.kt` | **New** — live `numeric/boolean/string_modifier(type, modifier)` via `Type:Name` encoding |
| `HedgeMazeConfig.kt` | **New** — shared hedge maze mode/pref helper (also used by Track C) |

---

## Track B — Rufus Shadow Labyrinth

| File | Change |
| ---- | ------ |
| `RufusManager.kt` | `ShadowTheme`, `shadowLabyrinthChoiceDecision`, `specialChoiceDecision` |
| `RufusHandlers.kt` | Choices 1498/1499 wired to labyrinth + hang-up logic |

---

## Track C — CLI `maze`

| File | Change |
| ---- | ------ |
| `GameRuntimeLibrary.kt` | `maze <mode>` visits nstower, guards FINAL step4, applies hedge prefs |
| `GameRuntimeLibrary.AshP20Batch.kt` | Delegates to shared `HedgeMazeConfig` |

---

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
.\gradlew.bat :androidApp:assembleDebug
```
