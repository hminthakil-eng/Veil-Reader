# Veil Reader — MAIN-FIRST BRANCH AND ARCHIVE POLICY
Effective 2026-10-11. This is the requested governance change; preserve existing open PR source and avoid forced or destructive cleanups.

## Naming
- `main` = single newest **integrated/evidence-qualified** development trunk. The oldest dormant `main` is not the default reference for new work.
- `feature/<area>-<ticket>-YYYYMMDD`, `fix/<area>-<ticket>-YYYYMMDD`, `qa/<area>-YYYYMMDD` = short-lived scope branches, normal reviewed PR targets main.
- `experiment/<area>-YYYYMMDD` = reversible product exploration; no production rollout or automatic promotion.
- `verify/<area>-<sha>` = disposable exact-head CI verifier. NEVER merge it; retain until logs/artifacts/evidence are linked.
- `archive/0001`, `archive/0002`, ... = monotonically numbered **immutable** previous main snapshots. Four digits for safe sorting. No force push; never use as a development base.
- `release/<version>` or version tags = ONLY independently approved, signed public release. Main being up-to-date does not authorize a public store push.
- Historical nonstandard branches must be inventoried, classified as active/dependent/landed/superseded/reject before archive. They cannot be safely renamed in bulk while PRs are open.

## Required promotion transaction
1. Verify main HEAD, candidate HEAD, commit ancestry, no active conflicting PR stack and an explicit accepted scope.
2. Confirm exact-head CI and security/status checks. For changes affecting touch/audio/visual/data, require the associated device/product evidence; no GREEN substitution.
3. Record proposed archive number N: fetch/archive list first, fail if name exists.
4. Create `archive/NNNN` from PREVIOUS main SHA; read back its SHA; preserve manifest and recovery command.
5. Update main with non-force fast-forward and expected old HEAD lease, or normal reviewed PR merge (with verified source). Reject any unexpected non-FF or SHA mismatch.
6. Rerun main CI, audit resulting SHA. Archive snapshot can restore through an explicitly reviewed rollback PR or a protected coordinated recovery; never secretly force-reset main.
7. Update Roadmap status + PR base references only after assessing stacked PR dependencies.
8. Close/retire only fully superseded branches AFTER PR status, references, and external workflows are checked. No mass delete. GitHub branches cannot literally be renamed by blindly copying refs when a PR target is still active; create archives/snapshots first, close PRs only by evidence.

## Before/after immutable manifest

| Archive | SHA | Prior role | Date | Notes |
|---|---|---|---|---|
| archive/0001 | cbf27c818409ad63d2c2fa55178bfd6c16a87642 | former main, 2026-09-20 | 2026-10-11 | Preserve pre-main-first state; replaced by main at 9c65997d |
| archive/0002+ | RESERVED (NOT CREATED) | future main snapshots | — | Allocate only before NEXT major accepted main promotion |

## Migration snapshot / warning
- Canonical old Reader branch head `grand-forge/p0-kindle-reader-quality-20261008` was ahead of previous main by 3,980 commits and **behind by 0**; main fast-forwarded to 9c65997d.
- Active branches like `grand-forge/reader-appearance-sheet-v1-20261011`, `grand-forge/inprocess-neural-tts-pcm-v1-20261011` and `grand-forge/open-source-paper-curl-arena-20261010` remain proposals; they are not individually ready just because date > main's date.
- GitHub branch details reported `protected=false` on old main; branch protection API returned insufficient integration permissions. Required checks/reviewer rights must be configured separately by a repository administrator.
- Some existing PRs are stacked intentionally (neural TTS #480→#482→#485). Retarget directly to main only when source ancestry and dependent PR graph are verified.
- Existing GitHub workflow branch filters still list historical development branches; follow-up cleanups must retain necessary verification coverage before retiring them.
- The archive rule is **non-destructive**. Archiving means preserving a named SHA, NOT asserting all other branch refs were deleted/renamed.

## Never
- Never claim current PR head = canonical main merely due to newest timestamp.
- Never make unverified experimental code the default app, and never turn Paper/TTS gates on to obtain screenshots.
- Never bypass main's required checks once configured.
- Never delete branches referenced by open PRs or scheduled workflows without a completed inventory and rollback.
- Never reinterpret a CI-success state as actual physical acceptance.
