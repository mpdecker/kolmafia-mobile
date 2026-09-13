# HTTP Residual Mega LIII (7031–7090)

> Tip: `phase7030` → wrap `phase7090`. No mid-track REVISION bump.

## Track A (7031–7050) — validate DI closure
- `CoinmasterAccessContext` + thread `ownsFamiliar` / `generatorQuestFinished` / underwater through `CoinmasterDatabase.containsBuyItem` / `containsBuySkill` / `CoinmasterPurchaseProbe` / AshP138 / `ConcoctionRefreshContext`
- GameRuntimeLibrary craft helpers for familiar + generator quest

## Track B (7051–7070) — token parse + buy glue
- Extend `MiscShopTokenResponseParse` (bacon BACON, glover G, boutique silver, blackmarket diamond, si_shop coins-spiracy)
- `CoinmasterResponseSync.apply` takes `gameDatabase` for `TinkeringBenchPurchasedItem`
- Wire visit hooks already call MiscShopToken

## Track C (7071–7090) — Crimbo polish + wrap
- CrimboHubResponseParse deepen (crimbo19toys token if missing)
- Markers/tests/corpus; bump `phase7090`; audit/AGENTS
