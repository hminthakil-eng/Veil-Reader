# R0.11 — SHA-linked Quality Dashboard

Date: 2026-10-05

The dashboard exists to prevent a recurring class of false claims:

- CI green is not device green.
- Device green is not product green.
- Screenshots from one SHA cannot validate another SHA.

## Automatic evidence

For an exact commit SHA the dashboard collects:
- Android CI
- Storage Instrumentation
- Performance Benchmarks
- Dependency Graph/Review when available
- presence of the Grayfog rendered-capture artifact
- packaged Baseline Profile proof inferred only from a successful sequential performance workflow

## Manual evidence

`quality/physical-device-status.json` is intentionally manual.

CI may read it but may never auto-promote it. If its SHA does not exactly match the inspected revision, physical evidence is considered stale/unverified.

## Status ceilings

Automation can produce at most:
- `GREEN-SOURCE`

Physical evidence can produce:
- `GREEN-DEVICE`

Only explicit product acceptance can produce:
- `GREEN-PRODUCT`

The dashboard intentionally reports product status as UNVERIFIED unless a future explicit approval mechanism is designed.
