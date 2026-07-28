# Phase 175: AshP145 Compound Craft Gates + AshP146 Coinmaster Validate v10

## Summary

Closed the largest remaining ConcoctionPermitted gaps (~100 compound-method recipes + 21 TINKER recipes) and added coinmaster validate v10 for bacon/arcade/dv/kiwi/fixodent shops.

## Delivered

### AshP145 — ConcoctionPermitted v4

- **`ConcoctionMethodAliases.kt`** — desktop `addCraftingData` token expansion (TINKER→GNOME_TINKER, WSMITH/ASMITH, SAUCE/SSAUCE/DSAUCE, ACOCK/SCOCK/SACOCK, PASTA/PASTAMASTERY/TNOODLE/TEMPURA, EJEWEL)
- **`ConcoctionCreationCost`** + **`ConcoctionPermitted`** — normalize methods before primaryMethod/skill checks; explicit allowlist (COMBINE/ACOMBINE true; STAR/SUGAR/PIXEL/TINKER false)
- **`ConcoctionMethodGates`** — gnomadsAvailable adds DesertBeach + KoE; CLIPART Bad Moon allows skillsRecalled
- **`CharacterState`/`CharacterApiResponse`/`KoLCharacter`** — `skillsRecalled` from api.php `recalledskills`
- **`GameRuntimeLibrary.AshP145Batch.kt`** — batch marker; REVISION `phase175`

### AshP146 — Coinmaster validate v10

- **`FolderHolderAccessibility.kt`** — folder holder equipped check (6617/11220)
- **`CoinmasterPurchaseAccessibility`** — bacon one-time prefs, arcade lockedItem/folder gates, dv per-character bought prefs, kiwi spirits one-time, fixodent monodent equip gate
- **`GameRuntimeLibrary.AshP146Batch.kt`** — batch marker

## Tests

- `ConcoctionMethodAliasesTest`, extended `ConcoctionPermittedTest`, `CoinmasterPurchaseAccessibilityTest`
- `GameRuntimeLibraryAshP145Test`, `GameRuntimeLibraryAshP146Test`
- Corpus: WSMITH/TINKER craft gates + bacon/fixodent validate snippets
- **3,135 tests green**

## Deferred (Phase 176+)

- Mystic psychosis pixel items (5906, 5907, 6173) — needs PixelRequest/shop sync
- FiveDPrinter / YouRobot / Robocore visitShop HTML sync
- Full CRIMBO05–12 legacy craft methods
- Arcade/kiwi/bacon visitShop pref refresh from HTML
