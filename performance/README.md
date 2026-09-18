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
