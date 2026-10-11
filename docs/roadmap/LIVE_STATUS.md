# VEIL READER — LIVE STATUS AND DECISION LEDGER
**Verified:** 2026-10-11 UTC; snapshot only. Needs continual reconciliation.
**Main SHA after promotion:** 9c65997d891b2b19ccba19e5729ce82bbdff9cce (before these documentation commits).
**Main:** https://github.com/hminthakil-eng/Veil-Reader/tree/main
**Read first:** [Grand Forge Master Roadmap](GRAND_FORGE_MASTER_ROADMAP.md)
**Overall product:** YELLOW / P0 PRODUCT BLOCKERS. No public-release permission and no all-feature GREEN.

The code and the user experience have different status columns. All entries below are grounded in cited PR or issue state observed this date; where device evidence is absent, state remains UNKNOWN or BLOCKED.

## ✅ Integrated in main — SOURCE ONLY; do not confuse with product acceptance

| Work | Source evidence | State after main migration | Device/product |
|---|---|---|---|
| Progress retry keeps durable origin | PR #451 merged to canonical before main promotion | INTEGRATED | UNVERIFIED on physical |
| Readium in-book search foundation | PR #452 merged | INTEGRATED | real-book large corpus QA still required |
| Search dialog/navigation | PR #455 merged | INTEGRATED | accessibility/IME/long text still requires device |
| In-book search input isolation | PR #461 merged | INTEGRATED | gesture conflict matrix still required |
| Paper input ownership, release-only commit | PR #467 merged | INTEGRATED | **BLOCKED: physical Paper not accepted** |
| Paper overlay touch-through / accessibility host | PR #474 merged | INTEGRATED | actual TalkBack and visual quality unverified |
| Paper mode/overlay eligibility | PR #475 merged | INTEGRATED | real-device Paper gate remains |
| PDF SAF Downloads fixture | PR #473 merged | INTEGRATED test fix | real PDF acceptance separate |
| Original prior main preserved | archive/0001 at cbf27c8 | ARCHIVED snapshot | no data overwritten |
| Development main fast-forward | main now 9c65997d (3,980 commits ahead of former main, behind=0) | INTEGRATED trunk move | not a release certification |

## 🟡 IN PROGRESS (draft or unmerged, each remains independently testable)

| Work | PR / known evidence | Blocker / next action |
|---|---|---|
| Real optical GPU deformation proof | #468; CI evidence exists, draft/unmerged | Samsung/Pixels physical footage, no Slide-like appearance |
| Ready-made curl engine comparison | #479; debug-only Apache-2.0 lab | test and compare; no EPUB integration nor product approval |
| Reader visual settings panel | #488; exact SHA 4cfedd6, Android CI/Storage/Performance success | real-device book sheet, 200% FA/EN, no locator drift; review |
| Image double-tap zoom | #487 | exact-SHA CI+real-device gesture conflict |
| TTS Pause / narrator consistency | #480 | audible phone evidence for Pause during load/background |
| Offline Kokoro/Sherpa bridge | #482 | inspect optional two-app cost, clear user controls, no false availability |
| In-process neural PCM runtime | #485; stacked on #482 | native real synthesis, model supply chain, audio latency/storage/quality |
| 120-frame Grayfog evidence gate | #466; test-only | full exact-head visual evidence + human design approval |
| Persian/English key/format parity | #464 | exact-head workflow; real typography/RTL device audit |
| World-class UX research | #489 | review and merge its evidence into main; no code change |
| Competitor skills/analysis | #428 #441/#448 | follow licensing, no closed-source copy |
| Book tooling, PDF and durability gaps | #409 #415 #417 | complete physical/edge-case acceptance |

## 🔴 BLOCKED / FAILED QUALITY CRITERIA

| Topic | Measured observation / why blocked | Unblock |
|---|---|---|
| Material Paper visual feel | Owner's real-phone report: currently seems Slide; #408 still open | video and real deformation/gesture acceptance after #468 vs #479 comparison |
| Neural TTS product | #485 is draft architecture, not proven in-process audible product | stable model + voice + playback/device acceptance |
| TTS Pause/voice | owner reported Pause ignored and narrator change; #480 draft | audible 10× lifecycle repetitions and proper controller ownership |
| True per-book appearance | #488 docs verify AppSettings.readerAppearance global-only | domain schema/inheritance, backup, migration and UX before switch |
| True product GREEN / public release | no verified real-device source-matched whole-app QA | P0 gates, signed release, performance/a11y/privacy review |
| main branch protection | API returned 403 reading protection; branch details show `protected=false` at promotion | enable reviewer/status checks via authorized GitHub settings/admin |

## ⛔ REJECTED BEHAVIOURS / SUPERSEDED APPROACHES

These are **decisions**, not claims that all corresponding branches were deleted.
- REJECTED: claiming Paper accepted because only Shader link/GPU upload/epoch or emulator CI passed.
- REJECTED: Paper silently becoming Slide/StaticPaged after failure; mode UI must state true effective capability.
- REJECTED: full-screen opaque appearance dialog with fictional specimen as the sole “live” preview; #488 proposes actual book in view.
- REJECTED: copying closed-source Kindle/Moon+ code, assets, voices or fonts without license.
- REJECTED: treating fresh PR timestamp or highest PR number as integration truth.
- REJECTED: archiving or deleting active branch dependencies during the migration.
- SUPERSEDED where integrated: isolated Paper gesture correction branches replaced by canonical integrated work #467/#474/#475; they remain as historical refs until independent archive audit.
- REJECTED: false user feedback (bookmark saved before durable Room commit, voice ready when provider unavailable, online text transfer without consent).

## ⏭️ NEXT: fixed execution order and individual decision gates

1. P0: physical Paper acceptance #408, candidate #468 vs #479. If either fails, keep RED and file per-device/motion reproduction. QA: exact APK signer/hash and 60/120Hz footage.
2. P0: audible Pause/voice stability #480. QA: foreground/background/lockscreen/Bluetooth/voice across 10 trials.
3. P0: durable progress and navigation/selection race. QA: real process kill after semantic commit, restore exactly, no user-facing false confirmation.
4. P0 UX: device-review #488. QA: actual FA/EN book behind sheet, 100/200% font, no lost settings/locator, visual reference approval.
5. P0 QA: #466 screenshot corpus and #464 i18n resource parity; do not confuse a tool gate with human/a11y acceptance.
6. P1: notes, highlight return, library scale/series filters, PDF fit/annotation, real per-book settings persistence, TTS in-process product.
7. P2: signature Grayfog world, camp→castle fair progression and original Persian/English fonts while preserving Reader performance.

## Daily report format

Date UTC and timezone; main head SHA; changed PR/issue list; integrated (source), device accepted (independently), blocked, rejected/superseded with reason; CI workflows status for exact SHA; physical evidence URL/hash; next P0 owner and action; archive number if any. Append a concise dated delta below rather than rewriting historical facts without traceability.

### 2026-10-11 — Governance migration
- PRE: main cbf27c8 on 2026-09-20.
- ACTION: snapshot previous main to archive/0001, then fast-forward main to 9c65997d with non-force expected SHA lease.
- CURRENT: newest **integrated** Reader baseline now main; active draft PRs retain experimental heads. No physical approval and no release.
- FOLLOW-UP: main protection was observed false; permission to read protection settings was denied, requiring separate authorized admin review.


### 2026-10-11 — GOV-08 pre-work and stage traceability (candidate PR #500)

- **Baseline checked before work:** main `bfac95f4b82e23b9f277f985bf3282f3e9c584fb`; command center #495 and roadmap, branch policy, existing Company OS reuse preflight and PR template inspected.
- **PRE-WORK receipt:** [GOV-08-W1](https://github.com/hminthakil-eng/Veil-Reader/issues/495#issuecomment-6107691747) was recorded before the PR opened. A draft implementation now lives at [PR #500](https://github.com/hminthakil-eng/Veil-Reader/pull/500).
- **Scope:** root AGENTS.md; mandatory execution protocol; two GitHub Action workflows; additive PR/work-item template changes; stdlib receipt engine and unit tests; command center mirroring of BOTH plans and stage outcomes.
- **Initial feedback:** first bootstrap preflight run failed because main had no tool tests yet. Workflow changed to use trusted exact head for bootstrap PR #500 only; later PRs use the reviewed main tool. Later [roadmap gate 38129419585](https://github.com/hminthakil-eng/Veil-Reader/actions/runs/38129419585) passed all 13 earlier tests and the live issue preflight check. Two more negative-control tests were added; **their exact newer SHA result requires a separate run**.
- **Stage disposition now:** IN_PROGRESS / not yet merged or deployed; GitHub main protections were observed disabled, so PR check is not a required status until repository administration enables it. Android runtime and public release have not changed.
- **Next:** confirm latest exact-head 15-test gate and relevant CI, publish POST-STAGE evidence on #495, review/merge the governance PR if safe, then validate workflow_dispatch on main and require status checks through GitHub settings.
