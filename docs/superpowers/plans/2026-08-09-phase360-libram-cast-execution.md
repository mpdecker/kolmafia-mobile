# Phase 360 — Libram Cast Execution (semicolon CLI + libramSummons sync)

## Context

Phase 358 emitted multi-libram ManaBurn `ManaBurnAction.Cli` semicolon commands, but mobile `dispatchCli` did not split on `;`. Phase 359 wired libram MP precompute with mana-cost adjustment.

## Goal

Execute multi-libram ManaBurn commands end-to-end and keep `libramSummons` in sync on every successful libram cast.

## Deliverables

1. **`dispatchCli` semicolon chain** — desktop `KoLmafiaCLI.executeLine` loop via `dispatchCliSegment`
2. **`LibramSkillCasts.libramSkillMpCostTotal`** — desktop batch MP helper
3. **`SkillManager.cast` libramSummons increment** — centralized pref sync; removed duplicate from `ManaBurnManager.burnIfEnabled`
4. **Tests** — semicolon CLI chain, `libramSkillMpCostTotal`, libram cast pref increment
5. **Audit** — `REVISION = phase360`, parity-audit Phase 360 history entry

## Verification

```powershell
.\gradlew.bat :shared:jvmTest
```

4,780 tests passing.
