# Premium Home × Gemini Code — Evidence (2026-10-14)

Branch: `design/premium-reader-world-v1-20261014`; source base: `e37869bb`; authorized Windows desktop.

- `PremiumReadingHomeScreen.kt` is the experimental replacement for `VeilTab.READING`; existing Reader, Library and Castle logic/routes still own actions.
- `ReadingHomeProgression.kt` and unit tests were reused from the prior worktree. Its pure no-write stages are CAMP, HUT, HOUSE and CASTLE.
- Gemini 4 Argon High on the Windows desktop received a *source-specific Kotlin implementation task*. Its complete code was copied using Gemini's **Copy code** control to `E:\EyadStudio\Temp\Gemini_Veil_Premium_Code_RAW_20261014.txt` (13,552 bytes), then placed in `PremiumWorldProgressionArt.kt`.
- ChatGPT wired Gemini's composable into the world gateway and constrained its height to 212dp to prevent unconstrained-scroll measurement issues.
- Reduced motion selects static ambient light and snap stage changes. All progression is derived from persisted reading counters.

## Verification
- The original premium home compiled with `:app:compileDebugKotlin`.
- After the Gemini module and integration, `:app:compileDebugKotlin --offline --no-daemon`: **BUILD SUCCESSFUL** (8 actionable tasks: 2 executed, 6 up-to-date, ~1m15s).
- There are existing Gradle multiple-daemon and deprecated-feature warnings.
- A targeted ReadingHomeProgressionTest run reached `:app:testDebugUnitTest` but lacked a successful result receipt before its session was stopped. Do NOT mark tests as passed.
- Visual screenshots, device/emulator tests, 60/120 Hz frametimes, font scaling, RTL/TalkBack and actual game feel remain UNVERIFIED.

## Follow-up
Run stage transitions at all four milestones, RTL and large fonts; measure draw allocation from house/castle window geometry; verify no animation competes with Reader paper mode. Keep this worktree separate and avoid production rollout pending acceptance.


## Second-pass engineering and package receipt
- Gemini authored a second-pass Kotlin optimization eliminating per-frame `listOf(Offset(...))` allocations for four House and five Castle windows. ChatGPT applied the same structural change to the real source and hoisted `Stroke` allocation into density-keyed `remember` values. Each stage now has isolated cached paths within the Crossfade content.
- Online Gradle `:app:assembleDebug` completed successfully after downloading uncached Android desugaring dependencies. The prior offline attempt failed from **missing dependency cache**, not source code.
- Incremental Gradle `:app:assembleDebug --offline --no-daemon --console=plain` with second-pass optimization: **BUILD SUCCESSFUL** in 1m07s, 40 actionable tasks (5 executed, 35 up-to-date).
- Exact verified local APK: `E:\EyadStudio\Projects\Veil-Reader-Premium-20261014\app\build\outputs\apk\debug\app-debug.apk`; 52,900,455 bytes; SHA-256 `D4ECC36BE31C0D3B351056231D30AF2F665DBCAF4C4F5A21E607B75D441081F2`.
- Windows ADB returned zero attached devices; physical and emulator visual acceptance therefore remain UNVERIFIED. This debug APK is a local test artifact, not a shipping recommendation.
