# W8 world and continuity source polish

Date: 2026-09-30
Base: 75f115ef509bbe927d38079a37640adf96a7065c
Track: alpha/w8-convergence-sync / PR #330
Scope: source refinement; no Android build or device verification.

## Changes

- Threshold uses the existing canonical book-artifact progress for its hero and recent status. Finished books with stale persisted progress show completed progress consistently with Library.
- Canonical artifact progress rejects non-finite stored values. Page-stack math also rejects non-finite direct inputs. Finishing remains authoritative at 100%; persisted reading positions are not modified.
- Threshold book selection is memoized against the incoming books list.
- Path ceremony visibility is scoped to Path ID and rank. Lost advancement eligibility closes the ceremony and cannot continue displaying a confirmation path.
- Advancement content is scrollable within its existing ceremonial frame, exposing actions on shorter screens and with larger fonts.
- Castle and Path width caps precede fillMaxSize; Profile now shares a capped, centered content measure.
- Profile fact labels use proportional width and values wrap instead of truncating to one line.
- Shared integer formatting supports localized zero padding. Castle floors, Path ranks, profile seal, counters, and dossier facts use it.
- Profile dates use the app resource locale instead of the process default locale.

## Source verification completed

- Reviewed the seven modified source/test diffs.
- Checked referenced string resources, conflict markers, and whitespace.
- Reused the existing English/Persian resource keys; no new wording or artwork.
- Canonical bookArtifactState remains the single progress presentation contract; no new progress database or reading engine.

## Coverage added, not run

BookArtifactTest adds two regression cases: malformed progress cannot contaminate stack geometry; completed status wins even over NaN stored progress.

Kotlin compilation, JUnit execution, Android rendering, and TalkBack/device checks were not run. This pass does not claim visual or runtime GREEN.

## Deferred acceptance checks

- Compare completed/stale-progress books on Home, Library, and Detail.
- Exercise the ceremony on a short landscape viewport and with large text; change Path/rank/eligibility while it is visible.
- Inspect Profile and Castle on compact/expanded displays in English and Persian.
- Run existing BookArtifact, Threshold, and advancement/mastery regression suites alongside the new cases.

