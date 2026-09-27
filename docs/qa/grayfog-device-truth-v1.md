# Grayfog Device Truth v1

Base: `f7c0be0cf311b6c2067d09122c651435ab2a6779` (`BUILD-WORKMODE-GRAYFOG-PERFECTION`).

## Purpose

The current Grayfog source has moved through Reader Sanctuary, Archive, Threshold, Castle,
Path, Sanctum, accessibility, reduced-motion, and RTL/Persian passes, but a screenshot or
APK is only useful evidence when it can be tied to the exact source revision that produced it.

This slice makes that provenance deterministic before the pixel-level visual gate.

## Build identity contract

Before APK assembly, `tools/build_identity.py prepare` writes a generated
`app/src/main/assets/veil-build.json` containing:

- full Git commit SHA;
- branch;
- Codemagic workflow ID;
- Codemagic build ID;
- UTC preparation timestamp.

The generated file is ignored by Git and is packaged into the APK as
`assets/veil-build.json`.

After assembly, `tools/build_identity.py verify`:

1. rejects dirty tracked source;
2. rejects unexpected untracked source under app/benchmark/buildSrc;
3. rejects a changed HEAD after preparation;
4. reads the identity back from the built APK;
5. rejects a stale/mismatched APK;
6. computes the APK SHA-256;
7. copies the verified APK to
   `build-evidence/Veil-Reader-<sha12>-debug.apk`;
8. writes `build-evidence/build-identity.json`.

Both WORKMODE-BUILD and WORKMODE-FULL-VERIFY use this contract.

## Scope boundaries

This change does not alter Reader behavior, Readium/PDFium ownership, Room, navigation,
visual composition, typography, motion, persistence, or production release policy.

It deliberately does not port the unrelated memory-scoring changes from the older audit PR.

## Gate after this slice

Run `WORK MODE — BUILD APK` on `qa/grayfog-device-truth-v1`.

A build is accepted for visual QA only when:

- policy/parser checks pass;
- build-identity tests pass;
- unit tests pass;
- lint passes;
- assembleDebug passes;
- `build-identity.json` says `identity_verified: true`;
- the artifact name contains the expected source SHA prefix.

Only then should screenshots be scored against the Grayfog reference board. Device visual
success is not claimed by this source-only slice.
