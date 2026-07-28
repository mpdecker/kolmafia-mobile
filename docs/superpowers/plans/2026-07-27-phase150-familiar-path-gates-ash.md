# Phase 150: AshP108 Familiar Path Gates + Maximizer Enthrone/Bjorn Usability

## Summary

Completed the familiar usability track deferred from Phase 149: aligned `FamiliarUsability` with desktop `usableFamiliar`/`usableFamiliars` (path `canUseFamiliars` + Quantum active-only rules), gated `use_familiar`/CLI/enthrone/bjornify actions, and filtered Maximizer enthroned/bjorn goals through the same helper.

## Delivered

- **`FamiliarUsability`** — `canUseFamiliars()` path gate, Quantum terrarium active-familiar-only rule, `firstUsableFromGoals()` helper
- **`resolveUsableFamiliarRace`** — shared refresh + lookup for ASH and CLI
- **ASH** — `use_familiar`, `enthrone_familiar`, `bjornify_familiar` gated through usability
- **CLI** — `familiar`/`enthrone`/`bjornify` skip HTTP when familiar not usable; `none`/`unequip` unchanged
- **`MaximizerManager`** — `resolveEnthronedFamiliar` / `resolveBjornifiedFamiliar` with `RestrictionListRefresh`
- **Tests** — `GameRuntimeLibraryFamiliarTest`, `GameRuntimeLibraryAshP108Test`, `MaximizerManagerTest`, `GameRuntimeLibraryCliTest`, corpus `corpus_useFamiliar_avatarPathBlocked`
- **`REVISION`** — `phase150` (2,748 tests)

## Deferred (Phase 151+)

- Bulk `AshCompatibilityCorpusTest` expansion
- AshP8–P18 interactive/PvP stubs
- Desktop `FamiliarCommand` lock/unlock/naked extras
