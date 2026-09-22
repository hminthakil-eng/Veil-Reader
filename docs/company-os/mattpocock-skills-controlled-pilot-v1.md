# Matt Pocock Skills Controlled Pilot v1

Date: 2026-09-22
Owner: Eyad Prompt & Skill Registry / SID
Target: Veil Reader Codex project skills
State: CONTROLLED PILOT

## Preflight

Company OS Work Reuse Gate was applied before installation.

Existing repository skills already cover:
- systematic debugging;
- test-driven development;
- requesting and receiving code review;
- verification before completion;
- Veil-specific Android engineering and release gates.

This means public skills are accepted only for a measurable capability delta. Parallel replacements are prohibited.

## Upstream provenance

Source: `mattpocock/skills`
License: MIT
Audited upstream snapshot: `c55ee46073ed923f86ce59a5eb3b6d895095d1b7`

The project-local install uses copied skill files for Codex. The lockfile records GitHub provenance and content hashes.

No automatic update is authorized. `npx skills check` may be used to discover changes, but any update requires a fresh targeted audit before `npx skills update`.

## Dedup decision

| Skill | Decision | Reason |
| --- | --- | --- |
| `codebase-design` | PILOT | Adds explicit deep-module, seam, interface, leverage and locality vocabulary not present as a dedicated reusable skill. |
| `code-review` | A/B PILOT | Adds explicit Standards vs Spec review axes and a fixed-point diff discipline. Must be compared against the existing Superpowers reviewer before promotion. |
| `diagnosing-bugs` | REJECT DUPLICATE | Substantially overlaps `systematic-debugging`; existing Veil reliability workflow already enforces evidence-first root-cause debugging. |
| `tdd` | REJECT DUPLICATE | Substantially overlaps `test-driven-development`; duplicate invocation would create policy ambiguity. |
| `grill-with-docs` | HOLD | Useful, but brings a larger dependency chain and domain-document workflow. Reconsider only after this smaller pilot is evaluated. |

## Guardrails

- Public skills do not own project architecture or Company OS policy.
- Public skills do not gain merge, deployment, credential, or production authority.
- Existing Veil-specific skills and release gates remain authoritative for product-specific constraints.
- If two skills trigger for the same task, the existing canonical owner skill wins unless the task is the documented A/B benchmark.
- Any copied upstream content remains third-party material under its original license.

## Pilot verification

`codebase-design` passes if it improves one real Reader architecture discussion without creating a parallel architecture or contradicting established ownership boundaries.

`code-review` passes only if, on the same fixed-point diff, it finds useful standards/spec evidence that the existing reviewer misses, without materially increasing false positives or review cost.

Recommended A/B target: one bounded Reader reliability PR against its canonical base. Run the existing reviewer and `code-review` independently, then compare unique actionable findings.

## Exit decision

After the benchmark, record exactly one outcome per skill:
- INTEGRATE;
- REUSE-PARTS;
- WATCH;
- REJECT.

If `code-review` does not produce a clear incremental signal, remove it and retain the existing reviewer. The pilot must not become permanent duplication by inertia.
