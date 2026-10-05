# R0.12 — Optional Feature Kill-Switch Contract

Date: 2026-10-05

## Rule

A release kill switch controls **availability**, never user data.

Turning a capability off must not:
- delete books;
- delete progress;
- delete highlights/notes/bookmarks;
- rewrite a user's stored preference merely because the renderer/backend is unavailable;
- run a destructive schema migration;
- silently move data to another owner.

## Central release gates

`VeilFeatureGates` currently tracks:
- GPU Material Page
- live Manga sources
- AndroidX PDF editor pilot
- cloud sync

All are release-disabled until independently promoted through Reforge evidence.

Only GPU Material Page currently has an implemented optional runtime that consumes the gate. The other entries reserve a single canonical admission point for future pilots and prevent ad-hoc booleans from appearing across the app.

## Paper behavior

If a user preference requests Paper while release availability is disabled:
- persisted preference remains Paper;
- effective presentation safely falls back to deterministic Paged + None;
- no user setting is erased;
- re-enabling a verified implementation can honor the original preference again.

Debug review remains separately controlled and cannot silently promote release availability.

## Promotion rule

A feature gate may switch release default to enabled only after:
1. GREEN-SOURCE;
2. subsystem-specific GREEN-DEVICE;
3. rollback test;
4. persistence/data safety review;
5. product acceptance where user-visible.

## Future remote/server switches

Do not add remote kill switches until there is an actual operational need. Veil is local/offline-first; a remote control plane must never become a requirement for normal reading.
