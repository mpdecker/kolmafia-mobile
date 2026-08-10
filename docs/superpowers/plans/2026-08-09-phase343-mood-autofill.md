# Phase 343 — Mood autofill minimalSet/maximalSet (TCRS applyModifiers v84)

## Context

Phase 342 completed mood `lose_effect` default-action routing (`phase342`, TCRS v83). The highest-impact deferral is desktop `MoodManager.minimalSet()` / `maximalSet()` mood autofill used by `mood autofill` CLI.

## Delivered

1. **`UneffectSkillEffectMap`** — desktop `EFFECT_SKILL` map from statuseffects `cast 1` actions + `skillToEffect`/`effectToSkill` lookups
2. **`SkillDefinitionProxy.isAccordionThiefSong`** — desktop `6001..6999 && isBuff` gate
3. **`MoodAutofill`** — `minimalSet`/`maximalSet`/`pickSkills`/`addActiveLoseEffectTrigger`/`canAutofill` on `MoodManager`
4. **`loadActiveMood` merge fix** — restores removal triggers from library entry on login
5. **CLI** — `mood autofill` → `maximalSet(...)` in `GameRuntimeLibrary.cliDispatch`
6. **Tests** — `MoodAutofillTest`

## Deferred (Phase 344+)

| Item | Reason |
|------|--------|
| `lose_effect` trigger execution in `executeActiveMood` | Desktop second-pass CLI action execution |
| Desktop `username_moods.txt` import | Separate persistence track |
| Full `skillToEffect` special-case switch tree | v1 reverse-map covers autofill songs/buffs |
| `editmood` / `saveasmood` CLI | Desktop edit UI commands |

## Verification

```powershell
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodAutofillTest"
.\gradlew.bat :shared:jvmTest --tests "net.sourceforge.kolmafia.mood.MoodManagerDefaultActionTest"
```
