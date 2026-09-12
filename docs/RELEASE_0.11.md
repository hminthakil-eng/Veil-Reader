# 0.11.0 — A Home Between Pages

Status: development candidate; Android CI verification pending at initial commit.

The earlier Castle was a rank display and room launcher. This change gives it a real construction loop: start in a shack, earn materials from engaged reading, build six successive upgrades, customize the illustrated result and follow an original quest line with resident dialogue and a persistent lore journal.

Reading gains explicit Slide / 3D curl (beta) / Instant / Scroll choices, typeface selection, text alignment, keep-awake and reduced page motion. Existing locators, annotations and the Readium renderer remain in charge of the book. Curl is experimental until physical-device checks confirm gestures, RTL, chapter boundaries and visual quality.

Persistence uses the existing game-state backup route and DataStore appearance route. No Room schema migration or new dependency/permission is introduced. Debug builds install as Veil Reader Preview alongside the existing app, so testing does not depend on the old debug signing key. The release application ID remains unchanged. The estate uses one saved JSON value for construction, spending and claims; appearance adds optional keys with safe defaults for older files.

Verification added:
- Campaign reachability, construction costs, single claims, ordering, unlocks and unrevealed lore.
- Android recreation and duplicate-claim tests.
- Full ZIP backup/restore of estate state and all new appearance settings.
- Curl rejection, overlapping input and disposal checks.
- Existing 39 reading-policy checks passed locally.

Outstanding: APK/unit/lint/emulator results at initial commit; native visual QA, physical-device performance and all remaining production gates in `docs/PRODUCT_PLAN.md`.
