---
name: veil-performance-release-lab
description: Benchmark Android startup, page-turn jank, memory, battery, large libraries, import, TTS and release artifacts with exact-head CI and physical-device proof.
---

# Performance / CI / Release Laboratory

## Benchmark contract
Record branch/head SHA, OS/API, model/chipset/RAM, build type, thermal state, test corpus and settings.
Use Macrobenchmark/Baseline Profiles, Perfetto/frame timing, memory profiling and the current performance harness. Do not infer GPU paper-curl quality from passing unit tests.
Establish an observed baseline and pre-agreed budget before calling a metric improved. Measure median, p95/p99 where relevant, dropped/janky frames, cold/warm startup, import throughput, memory growth and battery impact.
Use multi-condition corpora: tiny/long EPUB, mixed RTL, image-heavy fixed-layout, large PDF, 1000-book library, annotation-heavy and speech playback.

## Release requirements
Check exact-head Android tests, lint, optimized R8 APK/AAB packaging, Baseline/Startup Profile inclusion, committed migrations, accessibility, signing pipeline, reproducibility and physical-device smoke.
Avoid interpreting green from unrelated workflow run/commit; do not merge verification-only PRs. Preserve artifacts/logs sufficient to reproduce failures.
Never record or upload personal reading content, unredacted book passages or secret signing data with traces.

## Verdict
PASS / BLOCKED / UNKNOWN per gate, and link exact CI run, artifact hash, device capture, trace, owner and rollback. If the benchmark has no hardware evidence, mark that gate UNKNOWN, never GREEN.
