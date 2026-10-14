# Avatar Creator + Rags-to-Riches Vertical Slice — 2026-10-14

## Scope (not full 3D release)
This development-only vertical slice is implemented in a reversible design branch. It adds:
- `RagsToRichesAvatar.kt`: pure Kotlin, versioned built-in catalog slots, input normalization, four stable world stage IDs, and a non-decreasing **projection** of earned stage.
- `RagsToRichesAvatarTest.kt`: five pure-domain test cases for stage mapping, invalid-restored options, non-regression, real-reading counter dependence and engine independence.
- `AvatarAppearanceStore.kt`: cosmetic identity shares the `veil_game_v1` preference snapshot already exported and transactionally restored by `LibraryExport`. Existing `veil_avatar_pilot_v1` values migrate only if no restored identity exists. User changes still require explicit Save with a checked durable commit. Backup round-trip testing remains necessary.
- `PremiumAvatarCreatorScreen.kt`: reachable from Castle/World via the `avatar` restorable chamber; localized English/Persian; choice controls, actual user minutes, current computed home stage, and explicit save outcome.
- `PremiumAvatarPortrait.kt`: native static Compose Canvas anime-inspired *2.5D* fallback, avoiding any active 3D surface during book reading.
- Navigation only exposes the new editor if game is visible. No live 3D engine, entitlement economy, wardrobe unlock gating, sync, or asset-pack downloads have been introduced.

## Gemini co-development evidence
Gemini desktop returned an error on the original long Kotlin request. A smaller retry completed using the available Flash-Lite mode. Full independent Kotlin portrait source was retrieved through Gemini's desktop Copy-code control and saved to `E:\EyadStudio\Temp\Gemini_PremiumAvatarPortrait_Source_20261014.kt`. Its tapered anime-face geometry was incorporated into ChatGPT's detail-rich Canvas renderer. The standalone returned source is not falsely claimed as installed verbatim.

## Verification
- The initial Android source compile **passed** before importing Gemini-inspired face-shape edits.
- Updated source `:app:compileDebugKotlin` **PASS** with JDK 17 + Android SDK 37.0, offline Gradle on the desktop (2026-10-14). This is a compile receipt only.
- Targeted JDK 25 Robolectric tests (`AvatarAndWorldPersistenceTest` + `RagsToRichesAvatarTest`) compiled, but the worker stalled without producing a final JUnit report and was terminated. **NOT VERIFIED / NOT PASS.** A pure-domain JDK 17 retry was started separately; only an actual completed result can pass this gate.
- Windows AVD `VeilReaderQA_API35` launched headless for real UI check. No successful install/screenshot evidence yet.
- Cosmetic fields and the highest world-tier integer are now inside `veil_game_v1`, the existing v5 backup/restore preference snapshot. `GameRepository.publish()` reconciles the world high-water mark without Compose writes. Migration, restore/rollback round-trip and emulator acceptance are not yet independently verified.

## Acceptance gate checklist
- [ ] Five Rags-to-Riches pure domain tests pass on JDK21+.
- [ ] Full updated :app:assembleDebug passes.
- [ ] Emulator opens World > Customize traveler; at least one change visually alters hair/outfit/skin/accent and persists across app relaunch.
- [ ] RTL, narrow phones, 200% font scaling and screen readers tested.
- [ ] No Reader paper-curl, TTS, library or saved-locator regression.
- [x] Avatar cosmetics and world high-water now use existing `veil_game_v1` backup preference envelope; reconciliation is outside Compose. Source/compile confirmed; actual export → reinstall → restore QA still [ ].
- [ ] 3D runtime selection only after measured Filament/Godot rig + shading benchmark.

**Status:** prototype / unshipped; unchanged main branch.
