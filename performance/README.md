# Veil Reader performance budgets

Veil Reader uses AndroidX Macrobenchmark JSON as the source of truth for performance gates.

## Budget modes

- **smoke**: intentionally loose emulator guardrails. These catch catastrophic regressions, broken
  benchmark journeys, or a reader that has become dramatically slower. They are not product
  performance claims.
- **physical**: the product-performance gate for a stable representative physical device. These
  budgets should only be changed after an intentional performance review with traces.

The current physical targets are Veil Reader product budgets, not Android platform guarantees:
- Baseline-profile cold-start TTID median: <= 600 ms
- Reader frame overrun P95: <= 0 ms
- Reader frame overrun P99: <= 8 ms
- Reader CPU frame duration P95: <= 16.7 ms
- Reader CPU frame duration P99: <= 24 ms
- Reader journey must produce at least 20 frames so a no-op benchmark cannot pass.

## Running the gate

After Macrobenchmark has produced one or more `*-benchmarkData.json` files:

```bash
python3 tools/check_performance_budget.py --mode smoke benchmark/build
python3 tools/check_performance_budget.py --mode physical /path/to/physical-device-results
```

The checker searches recursively, supports AndroidX's emulator-prefixed benchmark names, prints every
budget check, exits non-zero on a missing metric or failed threshold, and writes a GitHub Actions job
summary when `GITHUB_STEP_SUMMARY` is available.

## Change policy

Do not relax a physical budget simply to make CI green. First inspect the Macrobenchmark trace,
confirm that the benchmark journey is still valid, and determine whether the regression is intentional.
If a budget changes because the product itself changed, record the reason in the pull request that
updates `performance/budgets.json`.


## Performance history and PR deltas

Every successful performance run on `main` publishes a normalized `current-smoke.json` artifact as
`veil-reader-performance-baseline`. Pull requests download the latest successful main baseline,
run the same benchmark journey, create a new normalized snapshot, and apply
`performance/delta-budgets.json`.

The delta gate is intentionally separate from the absolute budget gate:

- Absolute budgets answer: "Is this build still within Veil Reader's acceptable envelope?"
- Delta budgets answer: "Did this PR make an already-good build meaningfully worse?"

For timing metrics where percentage change is stable, the delta policy uses
`maxRegressionPercent`. For `frameOverrunMs`, which can be negative or cross zero, it uses an
absolute `maxIncrease` in milliseconds instead.

If no successful main baseline exists yet, a PR still has to pass all absolute smoke budgets. The
relative gate is skipped with an explicit job-summary notice; the next successful main performance
run seeds the baseline automatically.

Physical-device snapshots use the same normalized format and delta checker:

```bash
python3 tools/snapshot_performance.py \
  --mode physical \
  --device "pixel-reference-device" \
  --output performance/current-physical.json \
  /path/to/benchmark-results

python3 tools/check_performance_delta.py \
  --mode physical \
  --baseline /path/to/previous-physical.json \
  --current performance/current-physical.json
```

A baseline must come from the same benchmark journey and a stable representative device class. Do
not compare physical-device results across materially different hardware as if they were a code-only
regression.
