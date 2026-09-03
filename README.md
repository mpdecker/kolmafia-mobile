# KoLmafia Mobile

A **Kotlin Multiplatform** port of [KoLmafia](https://github.com/kolmafia/kolmafia)
— the long-running desktop automation tool for *Kingdom of Loathing* — bringing
its engine and ASH scripting runtime to Android and iOS. The desktop Java
codebase is the reference; this project reimplements it in Kotlin with a shared
core and native app shells.

---

## Status

| | |
| --- | --- |
| Default branch | `master` |
| Remote | `mpdecker/kolmafia-mobile` |
| Build | Gradle (Kotlin DSL), Kotlin Multiplatform |
| Targets | Android (`androidApp`), iOS (`iosApp`), shared KMP core (`shared`) |
| Tests | 8,000+ Kotlin `@Test` cases in `shared` |

Development is organized as numbered **phases** driven by a living parity audit
against the desktop tool. See [`docs/parity-audit.md`](docs/parity-audit.md) for
the current desktop-vs-mobile coverage metrics and the prioritized gap list —
it is the source of truth for what to build next.

---

## Layout

| Module | Purpose |
| --- | --- |
| `shared` | The Kotlin Multiplatform core — the KoLmafia engine, request/manager layer, and the ASH scripting runtime. The vast majority of the code and tests live here (`shared/src`, `commonMain`). |
| `androidApp` | Android application shell (`AndroidManifest.xml`, Compose UI). |
| `iosApp` | iOS application shell. |

Modules are wired in `settings.gradle.kts` (`:shared`, `:androidApp`); the iOS
app builds against the shared framework.

---

## Build

```bash
./gradlew :shared:build          # build and test the shared core
./gradlew :androidApp:assembleDebug   # Android debug APK
./gradlew :shared:test           # run the shared test suite
```

Use `gradlew.bat` on Windows. `local.properties` holds your local SDK paths and
is not committed.

---

## Working conventions

Per [`AGENTS.md`](AGENTS.md):

- Phase planning and gap-closing follow `docs/parity-audit.md` Top Priorities.
- After a phase, update `docs/parity-audit.md` with metrics and a history entry;
  a "parity audit" recount compares desktop-vs-mobile metrics, reprioritizes,
  and returns explicitly deferred items to the task queue.
- Plan files are read-only reference during implementation.
- Commits and pushes happen only when explicitly requested.

---

## Relationship to upstream

This is an independent Kotlin reimplementation, not a fork of the Java source;
parity is measured behaviorally (ASH overload coverage, `*Manager` / `*Request`
counts, banisher/effect enums, etc.) rather than by shared code. The upstream
desktop project is checked out separately under `../kolmafia`.
