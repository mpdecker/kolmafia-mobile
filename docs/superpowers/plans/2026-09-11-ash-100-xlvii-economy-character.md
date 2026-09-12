# ASH 100% Sprint — XLVII Economy+Character (6671–6730)

> **For agentic workers:** Deepen existing ASH only. Do NOT bump `REVISION` (parent wraps to `phase6730`). Exclude `git_*`/`svn_*`, `user_*`, Relay/JS, full xpath.

**Goal:** Close remaining ~25–40 economy/character shallow edges → honest ~100% live (~883 excl. non-goals).

**Desktop:** `C:\Development\kolmafia\kolmafia\src\net\sourceforge\kolmafia\textui\RuntimeLibrary.java`  
**Mobile tip:** `phase6670` → wrap `phase6730`

## Tracks (parallel; no REVISION bump)

### Track A — Mall / retrieve / buy / sell (6671–6690)
- `mall_price` — fifth-cheapest / age / forceUpdate edges
- `historical_price` / `historical_age` — mallprices.txt day-gate polish
- `retrieve_price` / `retrieve_item` — specialty craft residual
- `buy` / `sell` — using-storage / BOOLEAN overload residual
- `npc_price` — speakeasy/validate residual
- Marker: `GameRuntimeLibrary.Phase6671.kt`
- Tests: `GameRuntimeLibraryPhase6690Test.kt` + `corpus_behavioralDeepenXlviiTracksA_live`

### Track B — Shop / create / creatable (6691–6710)
- `put_shop` / `take_shop` / `reprice_shop` / `get_shop` / `shop_amount` — StoreManager batch/refresh
- `sells_skill` / `sell_cost` / `sell_price` — coinmaster skill cost residual
- `daily_special` / `well_stocked` — cafe residual polish
- `create` / `craft` — CreateItemRequest specialty residual
- `creatable_amount` / `creatable_turns` / `get_ingredients` / `concoction_price` / `craft_type` — cache/free-craft/MANUAL residual
- Marker: `GameRuntimeLibrary.Phase6691.kt`
- Tests: `GameRuntimeLibraryPhase6710Test.kt` + `corpus_behavioralDeepenXlviiTracksB_live`

### Track C — Maximize / modifiers / character / collections (6711–6730)
- `maximize` — exotic boost-source residual
- `numeric_modifier` / `boolean_modifier` / `string_modifier` — leftover entity/`Type:name`/`_spec` edges
- `get_property` / `remove_property` — global/user/System.* residual
- `refresh_status` / `restore_hp` / `restore_mp` — api/charpane/checkpoint residual
- `mood_execute` — inheritance residual
- `fullness_limit` / `inebriety_limit` / `spleen_limit` — path-base residual
- `available_amount` / `*_amount` / `get_free_pulls` / `get_no_pulls` — AccessibleItemCount residual
- `eudora` — AccountSync HTTP switch residual if thin
- Marker: `GameRuntimeLibrary.Phase6711.kt`
- Tests: `GameRuntimeLibraryPhase6730Test.kt` + `corpus_behavioralDeepenXlviiTracksC_live`

## Parent wrap
- Bump `"phase6670"` → `"phase6730"`
- Update `docs/parity-audit.md` + `AGENTS.md`
- Optional XLVIII only if residue remains

## Non-goals
HTTP residual; inventing non-desktop names; git/svn/user dialogs.
