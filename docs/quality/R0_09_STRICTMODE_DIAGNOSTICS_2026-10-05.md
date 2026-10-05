# R0.09 — Debug StrictMode Diagnostics

Date: 2026-10-05
Branch: `reforge/strictmode-diagnostics-v1`

## Purpose

Make accidental main-thread I/O and leaked Android resources visible during development/device QA without affecting release behavior.

## Installed only when BuildConfig.DEBUG

Thread policy:
- disk reads
- disk writes
- network
- custom slow calls
- resource mismatches
- log penalty only

VM policy:
- activity leaks
- leaked closables
- leaked SQLite objects
- leaked registration objects
- file:// exposure
- cleartext network
- unsafe intent launch on API 31+
- log penalty only

## Non-goals

- no death penalty;
- no release runtime overhead;
- no global suppression;
- no assumption that every log line is automatically a product bug.

## Follow-up

StrictMode findings must be triaged by ownership:
- app main-thread work;
- framework/library unavoidable work;
- test/debug-only work;
- known local development traffic.

A future cleanup may wrap explicitly justified synchronous durability barriers with the narrowest possible allowance rather than weakening persistence.
