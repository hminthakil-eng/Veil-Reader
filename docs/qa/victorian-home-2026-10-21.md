# Victorian station Home

Base: f43fa8cf038ca2ed3187078da057e70ef85793e3. Selected direction: user's ornate railway station reference (SHA-256 4c7488d99e2519778084e3819f865835f191663eca8fa31a8d428da7cb41f9f4).

## Intent
Resume an imported book, reopen recent books, and enter a specific library shelf. Preserve accepted reader, storage, signing and dependencies.

## Structure
Station doorway → current volume → recent volumes → four library entrances → existing whispers, mirror and reading record. Existing bottom navigation remains authoritative.

## Visual system
Midnight navy, antique brass, lamplight, live book covers and native readable text. Decorative artwork never contains interactive labels. English/Persian, RTL, scalable text, 48dp minimum actions, reduced motion and high contrast.

## State / ownership
Threshold snapshot owns resume/recent eligibility; library catalog owns shelf counts, including unavailable records. Navigation owns a consumable shelf request; entering a shelf clears conflicting search/collection/series filters. No invented remaining time or sample counts.

## Verification
HomeStationInteractionTest: one imported book → direct library entrance → callback once; four catalog records → shelf actions → counts 4/2/1/2 and four callbacks; 200% text/missing cover → resume → exact current book. First two expected to fail against base; third regression expected to pass. Observed RED on 11d31e162adc88d11571a329913d53ee713cc628, Android CI run 39619488420 / job 119288791387: 1074 tests, exactly two failures. Unit XML confirms zero matching nodes for the active Library entrance and All-volumes/4-books action. Large-text resume PASS; all other 1071 baseline tests PASS. Source verification artifact 12354731159, SHA-256 f07d9c358498e20c66d3da8b981eebd76f433fba79b6c278d98e44039d382a00. Setup failures are not RED evidence.

Existing configured Android CI supplies native verification because repeated workspace resets destroyed the supplied SDK/toolchain and interrupted execution before test compilation. Source edits remain in the sandbox. Mission: exact PR commit; commands and permissions unchanged from android.yml; outputs unit XML, lint, APK; inspect actual assertions and artifact identity before relying on results.

## Learning
Library filters include unavailable catalog records; only Home resume/recent require an imported source. Keep counts consistent with Library rather than silently applying Home eligibility. Previous runtime setup attempts produced no feature RED evidence and no production Home change. Publish this tests-first checkpoint before costly asset recovery.

Implementation checkpoint: native station art, dark brass current volume, recent covers and adaptive library arches; shared catalog predicates; SavedStateHandle-backed, consumable shelf navigation. Added restoration/stale-ack/filter-clearing tests and a 16-case emulator Home matrix with actual TextLayoutResult density checks. GREEN and rendered review pending. Asset hashes and bounded dimensions: victorian-home-assets.json. No physical-device or TalkBack acceptance claimed.
