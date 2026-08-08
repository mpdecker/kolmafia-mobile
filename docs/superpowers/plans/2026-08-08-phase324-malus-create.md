# Phase 324 — MalusCreateRequest (CreateItemRequest v11)

**Track:** TCRS `applyModifiers` v65  
**Revision:** `phase324`

## Goal

Add `MalusCreateRequest` for MALUS concoctions (elemental powder→nugget→wad via `guild.php?action=malussmash`), wire `isCreateSupported`/`ConcoctionCreateRequest`, and extend queue/`create` ASH coverage.

## Delivered

- `MalusCreateRequest` — POST `guild.php` with `action=malussmash`, `whichitem`, `quantity=1` per craft
- `ConcoctionData.isMalusCraftable()` + `isCreateSupported()` extension
- `ConcoctionCreateRequest` v11 router branch (before STILL fallback)
- `SharedModule` DI with `SkillManager` for Pulverize skill gate
- `MalusCreateRequestTest` + `ConcoctionCreateRequestTest.create_routesMalusToGuild`

## Deferred (Phase 325+)

- `DreadScrollManager.decorate()` — needs choice UI layer
- Choice 703 success quest sync
- `guild.php` visit-hook sync for manual `malussmash`
- Other exotic create methods: JEWELRY, SINGLE_USE, CRIMBO*, BARREL, etc.
