# Phase 72: `journeyman.txt` loader + `journey` CLI

**Date:** 2026-06-18  
**Track:** Data wiring + CLI  
**REVISION:** `phase72`

## Goal

Wire bundled `journeyman.txt` (1080 class/zone/skill tuples) and port desktop `journey zones` / `journey find` CLI for Journeyman path skill lookup.

## Deliverables

| Area | Change |
|------|--------|
| Loader | `JourneymanDatabase` — zone/skill maps + encoded location indices |
| Load | `GameDatabase.load()` calls `JourneymanDatabase.load()` |
| CLI | `journey zones <class>` and `journey find <class\|all> <skill>` |
| Tests | `JourneymanDatabaseTest`, CLI journey cases |

## Verify

`.\gradlew.bat :shared:jvmTest` ; `.\gradlew.bat :androidApp:assembleDebug`

## Deferred (Phase 73+)

- `journey zones` default-to-current-class when Journeyman ascension path exists
- Zodiac unreachable / known-skill polish in zones table
- MONSTER entity bracket fields
- Other unwired data files (`witchess_solutions.txt`, etc.)
