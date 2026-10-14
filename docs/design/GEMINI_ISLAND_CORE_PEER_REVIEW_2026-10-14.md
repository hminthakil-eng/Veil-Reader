# Gemini 4 Argon Peer Review — IslandShopCore v1
Date: 2026-10-14. Status: live response actually received and reviewed; proposals accepted selectively.
Source: desktop Gemini 4 Argon review of the actual IslandShopCore.kt copied through the UI to E:/EyadStudio/Temp/Gemini_IslandCore_Review_Partial_20261014.txt.
Reviewed in stacked Draft PR #520.

## Concrete Gemini findings and our decisions
- Duplicate book copy IDs in imported snapshot could mutate multiple records in a single replace(): ACCEPT; add validShopSnapshot() duplicate-ID check plus catalog/loan/order integrity.
- Sale counter and customer trust were Ints with missing overflow guards even though coins were bounded: ACCEPT; reject overflowing Sell/Lend. Assert previous state unchanged.
- Corrupted damaged on-shelf book was LENDable despite Sell validation: ACCEPT; require not DAMAGED in Lend.
- Starting copy IDs based on map index would shift on catalog reorder: ACCEPT; replace with stable explicit original book IDs (initial:fog_tales, etc), before any durable public game save exists.
- Shallow save validation: ACCEPT; check schema, nonnegative totals, duplicate IDs, loan ON_LOAN consistency, port-order manifest and available shelf capacity. Future tests must cover corrupted restore.
- Variable wholesale cost 70% of base price and increased trust for RESTORED condition: defer to economic playtest; not a correctness fix.
- Room save schema/revision/CAS + unique transaction row suggestion: architectural candidate for next task. No actual Room migrations, backup integration or concurrency proof completed; do NOT call it implemented.
- A clean-up of Kotlin Model/JSON serializer and GamePort best practice is design work, not implementation approval.
- Gemini suggested exact memory/performance thresholds in prior research without device samples; establish real measured baselines instead of asserting targets as real data.

## Separation of authorities
- Core Reader / EPUB, real book, Readium locator never imported by IslandShopCore.
- Existing GameRepository remains source of actual reading totals and Rank/Path; Rank controls castle rooms.
- AvatarAppearanceStore owns cosmetics; dual anime/pixel rendering is a projection only.
- Island game stores only fiction inventory, shop money, virtual days, loans, orders and story/relationship flags (future).
- Pure reducer has no I/O or durability; the repository adapter must atomically store state and applied command IDs. Never increment based on Compose recomposition.

## Peer-review acceptance process
1. ChatGPT applies only specific source-grounded patches after reviewing them.
2. Rerun the exact focused suite and confirm new JUnit XML on exact branch head.
3. Commit/push to isolated PR #520; keep DRAFT. Share with Gemini for next independent review on subsequent cycles.
4. Next responsibility split: Gemini designs alternative engine test/content schemas; ChatGPT builds transaction boundary + backup migrations and strict reader regression tests.
5. Production merge remains blocked until actual device metrics and playable shop scene.

## Readiness
Core source+unit tests: verifier records in qa/evidence/ISLAND_SHOP_CORE_V1_20261014.md.
Game persistence/production gameplay: BLOCKED, not yet implemented.
