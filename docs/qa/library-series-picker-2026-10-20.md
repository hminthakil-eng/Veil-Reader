# Series picker execution ledger

Approved scope: complete searchable series selection in the existing Android Library. Preserve query/sort, collection semantics, reader engine, storage, schema, dependencies and application identity.

Durable base: ec02ec2106590ddf85bc15b802b3ae21a90a0a48 (PR #557).
Plan: verify production UI regressions; replace eight-preview menu with canonical lazy searchable dialog; verify grouping and large lists; run Android CI and accessibility checks; review and retain a draft PR.

Recovery ruling: the sandbox execution environment became unavailable on 2026-10-20. Uncommitted picker files and the final test process cannot be recovered through current capabilities. This branch is a new implementation against the exact durable base, not a byte-identical transport of those files. No previous GREEN result is asserted.

Mission: GitHub's existing pull-request workflows provide bounded remote verification because no usable sandbox execution environment is available. Inputs are this branch's exact commits; workflow permissions and retention remain unchanged. No custom privileged workflow or paid infrastructure is introduced.

Step 1: Four production Compose regressions added before implementation. Expected RED: missing dialog/search/full choices. Existing seven cases retained. Await fresh Actions results.

Step 2 implementation prepared: canonical case/trim grouping shared with shelves; separate precomputed Persian search normalization; deterministic names and distinct volume counts; lazy keyed options; localized no-match and clear-search actions; All series stays available under search; radio selection semantics; wrapping labels and 48dp minimum actions; dialog-local IME focus handling. Nonblank series selection clears collection as before. All series and Cancel preserve collection, query and sort.

Additional coverage: five pure choice tests including 10,000 series, identity/count invariants and Persian variants; one production IME-focus interaction; parameterized device test across English/Persian, 100/130/150/200% font and standard/high contrast (16 combinations), with dialog capture. These checks are pending execution.

Pre-flight: the choice model feeds both canonical filter reconciliation and picker options. Existing shelf preview limits remain presentation-only. No changes to reader, database, dependency graph or runtime toolchains.

Final source review (exact ec02ec2..265878d): fresh independent reviewer found no Critical/Important production defects. Canonical completeness, grouping/search separation, stable lazy keys, localized actions and host behavior confirmed by inspection.

Final: minor (deferred): device test constrains the dialog card, not the separate window; actual compact-window and IME-open layout remain unverified. The Persian Robolectric interaction explicitly uses a 320dp qualifier and 200% font, but is not device screenshot evidence.
Final: minor (deferred): selection/cancellation tests do not explicitly exercise a nondefault collection and explicit sort; preservation/clearing verified in host source, not dedicated interaction assertions.

Declined runtime judgments remain pending: compilation, GREEN suite, real TalkBack, screenshot inspection, exact emulator window dimensions and measured performance. A 10,000-choice functional test is not a latency benchmark.

Learning: shelf preview limits must not determine retrieval completeness; keyboard search equivalence must not redefine persisted identity; Dialog layout/focus must be checked within its own window. Project-specific evidence remains here for the next skill-maintenance pass.

Observed RED: Android CI run 39498819871, job 118890759084, exact head 8c77b39ad7997c7222c77d83f13769534c47621b. Full command gradle :app:testDebugUnitTest --stacktrace completed with 1064 tests, exactly four failures, all newly added production interactions:
- seriesSearchAcceptsPersianKeyboardVariantsAtLargeFont (missing search field, line194)
- seriesChooserShowsCountsSelectedStateAndClearSelection (missing dialog row, line206)
- cancelledSeriesSearchRetainsLibraryQueryAndSelection (missing dialog row, line222)
- seriesOutsideEightPreviewCanBeSelected (missing full-choice dialog list, line176)
The existing 1060 tests passed. Build failed in 9m6s as intended; this is RED evidence, not a successful application build. Policy, Paper guards and performance/release parser checks passed before unit execution.

Candidate verification run 39500213598 stopped at test compilation: LibraryRetrievalInteractionTest.kt:241 chained assertIsNotFocused after performImeAction. Compose's performImeAction returns Unit, not SemanticsNodeInteraction (compiler receiver mismatch). The same chain existed in the added device case. Split the focus assertion, IME action and subsequent focus assertion into separate calls in both tests; assertions are retained. Production code is unchanged. Re-run full configured workflows on the corrected exact head. This failed compile is not GREEN evidence.
