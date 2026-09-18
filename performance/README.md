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


## Performance history, dashboard and PR deltas

Successful performance runs on `main` persist normalized smoke history on the dedicated
`performance-history` branch. This avoids depending on GitHub Actions artifact storage for the
baseline itself. The branch stores:

- `performance/history/smoke-history.json` — up to 30 normalized snapshots.
- `performance/history/dashboard-smoke.md` — the latest human-readable trend dashboard.
- `performance/history/dashboard-smoke.json` — machine-readable trend status.

Pull requests load that persisted history, run the same benchmark journey, append the current
snapshot in-memory, and then apply both absolute and relative checks. The persisted history is only
updated after a successful performance run on `main`.

The delta gate is intentionally separate from the absolute budget gate:

- Absolute budgets answer: "Is this build still within Veil Reader's acceptable envelope?"
- Delta budgets answer: "Did this PR make an already-good build meaningfully worse?"
- Trend detection answers: "Is performance drifting in the wrong direction across several runs even
  before a hard budget or PR delta is crossed?"

For timing metrics where percentage change is stable, the delta policy uses
`maxRegressionPercent`. For `frameOverrunMs`, which can be negative or cross zero, it uses an
absolute `maxIncrease` in milliseconds instead.

Trend detection uses `performance/trend-policy.json`. It requires multiple recent points and a
sustained worsening sequence before emitting a **watch** warning. Emulator trend warnings are
informational rather than hard failures because hosted-emulator timing is noisy. Absolute smoke
budgets and PR delta budgets remain the enforced CI gates.

If no persisted main history exists yet, a PR still has to pass all absolute smoke budgets. The
relative gate is skipped with an explicit job-summary notice; the next successful main performance
run seeds the `performance-history` branch automatically.

Raw trace/report upload remains best-effort only. If GitHub Actions artifact storage is full, the
performance gates, persisted history and dashboard can still function.

Physical-device snapshots use the same normalized format, delta checker and dashboard builder:

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

python3 tools/build_performance_dashboard.py \
  --history /path/to/physical-history.json \
  --snapshot performance/current-physical.json \
  --output-history performance/updated-physical-history.json \
  --markdown performance/dashboard-physical.md \
  --json performance/dashboard-physical.json
```

A physical baseline and history must come from the same benchmark journey and a stable representative
device class. Do not compare materially different hardware as if the change were a code-only
regression.
