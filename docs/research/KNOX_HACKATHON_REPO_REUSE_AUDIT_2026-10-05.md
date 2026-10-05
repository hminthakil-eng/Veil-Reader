# Knox Hackathon Kit — Veil Reader Reuse Audit

Status: CONTROLLED REUSE ADOPTED  
Date: 2026-10-05  
Base reviewed: `integration/page-engine-canonical-v1`  
Scope: Veil Reader + Eyad Studio Company OS

## Why this exists

Roman Knox's September 2026 hackathon kit was reviewed as an execution-method reference, not as an instruction to import hackathon code blindly. The source kit highlights three repositories and a build discipline centered on rapid scope lock, live smoke tests, saved fixtures, early deployment, feature freeze, and demo survivability.

Veil Reader is not a hackathon app, so the useful parts are adapted to our Android/release context rather than copied literally.

## Upstream review

### 1) public-apis/public-apis

Source: https://github.com/public-apis/public-apis  
Observed 2026-10-05:
- actively updated
- MIT licensed
- large curated API directory
- relevant categories include Books, Dictionaries, Documents & Productivity, Cloud Storage, CI, Open Source Projects
- book-oriented candidates visible in the current list include Open Library, Google Books, Gutendex, Crossref metadata, Ganjoor and others

Decision: **ADOPT AS UPSTREAM DISCOVERY REFERENCE**

Allowed use:
- SID/API discovery
- metadata/source research for future optional integrations
- shortlist generation for experiments
- smoke-testing candidate public APIs before design work begins

Not allowed:
- treating a README entry as proof that an endpoint is reliable
- adding a network dependency to the Reader core without explicit product need
- weakening Veil Reader's local/offline-first behavior

Required gate for any candidate:
1. verify terms/license
2. smoke-test a real endpoint
3. record latency/failure behavior
4. define offline/fallback behavior
5. prove value over existing local functionality

### 2) Aadiii00/hackathon-tools

Source: https://github.com/Aadiii00/hackathon-tools  
Observed 2026-10-05:
- last push: 2026-01-24
- no repository license detected
- useful mainly as a categorized tool index

Decision: **REFERENCE ONLY — NO CONTENT/CODE IMPORT**

Reusable idea:
- one tool per lane
- lock the tool choice before execution
- avoid repeated tool-comparison churn during implementation

Veil adaptation:
- use Company OS ownership layers and existing approved stack as the authority
- do not add a second tool for the same job unless an explicit benchmark/pilot proves a gap
- every new tool still goes through Work Reuse Gate + license/provenance review

### 3) geekedparker/hackathon-projects

Source: https://github.com/geekedparker/hackathon-projects  
Observed 2026-10-05:
- last push: 2026-09-28
- no repository license detected
- most entries are tutorial/project links; the strongest reusable concept is the compact five-part project brief

Decision: **STRUCTURAL INSPIRATION ONLY — NO CONTENT/CODE IMPORT**

Useful structure:
1. problem
2. user stories
3. MVP / minimum end-to-end outcome
4. bonus scope
5. resources

Veil adaptation:
- this structure is already substantially covered by `docs/company-os/work-preflight-template.md` and `docs/company-os/work-reuse-gate-v1.md`
- therefore we do not create a parallel backlog format
- when a future Veil experiment lacks a crisp definition, use the existing Work Preflight and keep the same brevity discipline

## Knox method adapted for Veil Reader

### A. Smoke-test before commitment

For any external API/tool/library:
- run a minimal real integration test first
- capture evidence
- do not architect around an unverified upstream

### B. Fixture/fallback discipline

The kit recommends saving a successful API response immediately.

Veil equivalent:
- preserve deterministic regression fixtures for network-backed optional features
- preserve sample EPUB/PDF assets and expected reader-state evidence for critical Reader paths
- test failure/offline paths deliberately

### C. Early executable truth

The kit says deploy early instead of waiting until the end.

Veil equivalent:
- produce an installable APK/AAB or emulator/device-verifiable build early in the slice
- UI screenshots alone are not proof
- a branch is not GREEN until the target path runs on a real build and the relevant gates pass

### D. Feature freeze

Veil release/hardening work adopts the same principle:
- after release-candidate freeze, only blocker fixes, correctness fixes, packaging fixes and evidence work enter the candidate
- new feature ideas move to backlog/next wave

### E. Demo-path ownership becomes reader-path ownership

For Veil, the equivalent of the hackathon demo path is the canonical reading path:
`Library → open book → render → navigate → background/restore → close/reopen at durable location`

Any change touching Reader must preserve this path.

### F. Secret scanning

The kit recommends scanning full Git history before publishing.

Decision:
- adopt as release/security hygiene
- use a secret scanner such as gitleaks in a controlled CI/security pass
- do not block current Reader engineering until the workflow is separately reviewed

## Reuse outcome

| Upstream | Outcome | Veil use |
|---|---|---|
| public-apis/public-apis | ADOPT REFERENCE | API/source discovery only; per-candidate verification required |
| Aadiii00/hackathon-tools | REFERENCE ONLY | one-tool-per-lane discipline; no import |
| geekedparker/hackathon-projects | STRUCTURE ONLY | brief discipline through existing Work Preflight; no import |
| Knox execution method | ADAPT | smoke test, fixtures, early executable truth, freeze, fallback, secret-scan hygiene |

## Non-duplication ruling

No new parallel process is created.

This audit extends:
- `docs/company-os/work-reuse-gate-v1.md`
- `docs/company-os/work-preflight-template.md`
- `docs/registry/license-provenance-gate-v0.1.md`

Those remain canonical.

## Next trigger

Re-open this audit only when:
- a Veil feature genuinely needs an external API,
- a new build/tool lane has an unresolved gap,
- or upstream licensing/content materially changes.

Otherwise: **do not browse these lists repeatedly. Reuse this decision.**
