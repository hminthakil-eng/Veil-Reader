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
HomeStationInteractionTest: one imported book → direct library entrance → callback once; four catalog records → shelf actions → counts 4/2/1/2 and four callbacks; 200% text/missing cover → resume → exact current book. First two expected to fail against base; third regression expected to pass. Execution pending, setup failures are not RED evidence.

Existing configured Android CI supplies native verification because repeated workspace resets destroyed the supplied SDK/toolchain and interrupted execution before test compilation. Source edits remain in the sandbox. Mission: exact PR commit; commands and permissions unchanged from android.yml; outputs unit XML, lint, APK; inspect actual assertions and artifact identity before relying on results.

## Learning
Library filters include unavailable catalog records; only Home resume/recent require an imported source. Keep counts consistent with Library rather than silently applying Home eligibility. Previous runtime setup attempts produced no feature RED evidence and no production Home change. Publish this tests-first checkpoint before costly asset recovery.
