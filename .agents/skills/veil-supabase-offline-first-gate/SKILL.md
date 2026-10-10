---
name: veil-supabase-offline-first-gate
description: Evaluate Supabase only when Veil Reader has an explicitly approved optional sync, auth, backup or cloud need; preserve offline-first Room/DataStore ownership, user consent, privacy and strict access control.
---

# Veil Reader — Supabase Adoption Gate

## Source order
Read `EYAD_STUDIO.md`, `ENGINEERING_BLUEPRINT_1.0.md`, `.agents/skills/veil-android-engineer/SKILL.md`, repository canonical branch/PRs, and the official installed `.agents/skills/supabase/SKILL.md`. This skill governs **project-specific authorization**; the official Supabase skill offers technical guidance only. The native app is Kotlin/Compose and must not import JavaScript/React backend clients.

## Default state: no remote backend
- The offline-first Reader, existing Room durable data, and DataStore reader preferences remain sources of truth; imported user books remain app-private unless a user explicitly exports/syncs.
- No cloud dependency, Supabase project, table, schema migration, auth configuration, storage bucket, telemetry SDK or user book upload is authorized just because the official skill or ChatGPT plugin is available.
- Do not put Supabase credentials, personal access tokens, database URL, service_role key, JWT secrets, or book contents in GitHub, prompts or hosted tooling.
- If cloud feature work is explicitly approved: write an architecture decision record covering device-owned state, conflict/merge semantics, encryption, consent, deletion/export, outage behavior, cost limits, threat model, recovery, rollback, storage region and legal/privacy implications before touching production services.

## Future gated workflow
1. Name a user-approved cloud feature (e.g. optional account/sync of progress and highlights, not mandatory sign-in).
2. Compare current local-only architecture and at least one alternative. Confirm Supabase improves the user outcome, not merely availability of a plugin.
3. Use official Supabase documentation, exact SDK/CLI versions and security guidance. No direct mobile access to server/service keys; RLS required on exposed tables, with verified ownership predicates and negative tests across user accounts.
4. Develop against a disposable scoped sandbox, never production by default; apply migration only with rollback, backup and explicit approved scope.
5. Demonstrate offline continuity, conflict reconciliation and process-death/backup durability on exact commit and real Android devices. Release gates remain independent.

## Ownership
Design research may reference Supabase capabilities; agents cannot unilaterally create external resources or shift Veil Reader's storage model.
