# Series dialog accessibility inheritance

Source under investigation: e182b46cdf982383c96bbe26dfc0dc521263c08c, draft PR #558.

## Finding

The previous source passed all six configured workflows: Android CI 39502326161, Storage Instrumentation 39502326165, Performance Benchmarks 39502326179, Dependency Baseline 39502326174, Dependency Review 39502326354 and Quality Dashboard 39502326350. Artifact inspection confirmed 1070 unit tests and 193 device tests with no failures, errors or skips. This does not establish that the intended font scales reached the dialog.

Device artifact 12301927946 was downloaded and verified against SHA256 4de538f7c89f991cc74ed7d68f3142bbe246cb0d66b2cb7d15eb35422803f622. For each English/Persian and standard/high-contrast pair, RGB ImageChops.difference between the 100% and 200% series-picker captures returned no bounding box: the pixels were identical. Consequently the prior parameterized font coverage is invalid evidence of enlarged dialog text.

The test fixture supplies LocalDensity outside a Dialog, which creates its own Compose owner. Investigate whether this boundary replaces the caller density. The new regression reads the actual TextLayoutResult density of a series label; the device matrix must assert the same leaf value before capturing. A parameter name or screenshot filename is insufficient.

## Scope and remaining verification

Keep focus and keyboard providers inside the dialog: they belong to its distinct focus root. Preserve the complete caller Density object if inheritance is required, including its font conversion behavior. Do not change the reader engine, storage, dependency versions, signing or application identity.

## Observed RED and bounded correction

Before production edits, the scoped regression completed on the sandbox: one test, one assertion failure, no errors. The actual series-label layout used fontScale 1.0 while the caller requested 2.0 (LibraryRetrievalInteractionTest line 264). Build duration 4m32s included fresh toolchain dependency acquisition. The earlier plugin connectivity failure was an environment/setup failure, not RED evidence; routing Java Maven requests through the existing proxy resolved it without repository dependency changes.

The correction captures LocalDensity before opening Dialog and supplies the same object around its content. Dialog-local focus and keyboard services are retained. Preserving the object avoids discarding custom/platform font conversion. Scoped GREEN: all 13 LibraryRetrievalInteractionTest cases and five LibrarySeriesChoiceTest cases passed, with zero failures/errors/skips. This includes the same actual-text 2.0 assertion that failed at 1.0 before the fix. compileDebugAndroidTestKotlin also succeeded. The successful combined build took 1m27s. Full unit, lint and debug build verification is running; device capture execution remains pending.

Independent source review of the actual production/test diff found no Important issue. The current test fixtures use a synthetic linear Density, so they do not establish Android nonlinear system-font conversion or physical-device acceptance. Actual compact-window/IME-open behavior and physical-device TalkBack remain separate verification gaps.
