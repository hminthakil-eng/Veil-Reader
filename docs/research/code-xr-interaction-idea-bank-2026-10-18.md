# Veil Reader — Code.XR Interaction Idea Bank

**Research date:** 2026-10-18  
**Scope:** Public Instagram posts by [@code.xr](https://www.instagram.com/code.xr/), [the creator's component storefront](https://topmate.io/code_xr), and independently researched open-source/native implementation options.  
**Status:** Ideas and implementation research **only**. No code, assets, products, or licenses were purchased or installed. The Instagram videos did not reliably play in the available public viewer; for individual demos, the creator's published descriptions/titles were examined, **not frame-by-frame video playback**.

## 0. Executive direction / راهبرد اصلی

Preserve the app's true **offline-first reader** and its separate `PAGED`, `PAPER CURL`, `SLIDE`, and `SCROLL` modes. Adopt UI *interaction principles*, not visual clones. Dark, mysterious, victorian/steampunk, restrained brass, red stars in mist, and a parchment reader canvas. Animation should tell a story and confirm interactions, but never delay opening a book or interrupt reading.

**Research coverage:** 15 specific public Instagram reel descriptions, the creator's storefront catalog (a bundle was advertised as 78 products at time of research), plus open Android/web animation resources. A storefront item is evidence that a product is listed, **not** proof of a working implementation or usable license.

## 1. Evidence: directly attributable Code.XR posts

| ID | Published public demo description | Date | Source | Original technology / relevant notes |
|---|---|---|---|---|
| CR01 | Apple-style glass navbar | 2026-09-21 | https://www.instagram.com/reel/DdjQL6hT9ho/ | React + CSS + Framer Motion; responsive, micro-interactions |
| CR02 | Glassy carousel | 2026-10-08 | https://www.instagram.com/reel/DeO_5_FTXn2/ | React + CSS; card transition |
| CR03 | Hanging cards / swing | 2026-09-24 | https://www.instagram.com/reel/DdqR1RNTRXD/ | React + CSS + Framer Motion; physical swing |
| CR04 | Animated OTP verification V2 | 2026-05-30 | https://www.instagram.com/code.xr/reel/DY9pE1qzMhv/ | Input focus / state feedback; low relevance to reader |
| CR05 | OTP verification V5 | 2026-07-22 | https://www.instagram.com/code.xr/reel/DbGJsr5TFxN/ | Focus movement; reusable micro-feedback principles |
| CR06 | Animated payment checkout | 2026-07-06 | https://www.instagram.com/code.xr/reel/Dac7xfXzudX/ | Staged success choreography; NOT a reader checkout requirement |
| CR07 | Lamp toggle login | 2026-06-29 | https://www.instagram.com/code.xr/reel/DaK6PRHT1f_/ | Interactive light reveal; adapt to reading-lamp ambience |
| CR08 | Glassy login V3 | 2026-10-18 | https://www.instagram.com/code.xr/reel/DeoGCPlzKOs/ | Neon glass login; use restraint; login not required by offline-first app |
| CR09 | Modern animated dashboard | 2026-10-17 | https://www.instagram.com/code.xr/reel/DemzgbITYVM/ | Animated analytics; adapt to reading/progression |
| CR10 | Fashion product landing page | 2026-10-17 | https://www.instagram.com/code.xr/reel/DemIQ9MT2fd/ | Product staging; adapt carefully to featured book layouts |
| CR11 | Glassy sidebar V3 | 2026-10-17 | https://www.instagram.com/code.xr/reel/DejnEUMzOdQ/ | Sidebar / hover; replace hover with mobile gestures and accessible tap |
| CR12 | Lamp login V3 | 2026-10-15 | https://www.instagram.com/code.xr/reel/DehCgLjTpRo/ | Interactive lighting |
| CR13 | 3D driving game / interactive environment | 2026-10-15 | https://www.instagram.com/code.xr/reel/Degex2YTCfD/ | React + Three.js; proof of 3D demo concept, **not** a native Android engine |
| CR14 | Login + OTP animation | 2026-10-14 | https://www.instagram.com/code.xr/reel/DeefFnvTwrm/ | Animated multi-stage state transitions |
| CR15 | Futuristic glass dashboard V13 | 2026-10-13 | https://www.instagram.com/code.xr/reel/Deb5ID8Tfe7/ | Glass + neon analytics; avoid over-lighting the reader |

**Commercial component catalog:** https://topmate.io/code_xr (categories include glass carousel, hanging card, navigation tabs, apple dock, animated upload, search, file upload, particle animation, MacBook scroll, SVG icon animation, dashboards, sidebars, cart, payments, lamp login and many OTP/login variations). The advertised package: https://topmate.io/code_xr/2263396. Prices, inventory and bundle contents can change; do not assume commercial or redistribution rights without license review.

## 2. Prioritized native interaction concepts (the reusable idea bank)

**Legend:** `P0` build an isolated prototype first, `P1` good follow-up, `P2` exploration only, `X` intentionally defer. `CRxx` references the documented creator's original demo; `STORE` references creator product listings; `ORIGINAL` is our own concept inspired by the design direction, not presented as the creator's work.

| ID | Priority | Interaction / original or source | VEIL application | Acceptance criteria / guardrail |
|---|---|---|---|---|
| VR-01 | P0 | Reader chrome spring-reveal — CR01 | Center tap reveals controls; tap outside dismisses | Never cover critical lines for long; no layout shift in page content |
| VR-02 | P0 | Glass library carousel — CR02 | Continue Reading + featured shelves | Clear snapping, instant open, keyboard/TalkBack focus |
| VR-03 | P0 | Physics hanging cards — CR03 | Artifacts, quests, achievements in Castle only | Movement responds to touch and settles; no perpetual swinging |
| VR-04 | P0 | Lamp as diegetic control — CR07/CR12 | Tap brass lamp to preview reading lighting / theme | Settings remain accessible without hunting for object |
| VR-05 | P0 | Sidebar morph / reveal — CR11 | Archive/collection navigation | Drag/tap/back all reversible; no hover-only affordances |
| VR-06 | P0 | Search-bar expansion — STORE: New Search Feature | Instant library and in-book search | Return focus/results correctly; respect IME and RTL |
| VR-07 | P0 | Animated import/progress — STORE: Upload Button, File Upload V2 | Book materializes on shelf after successful import | Animation not before persistence; error/retry state clearly distinct |
| VR-08 | P0 | Star-in-fog object selector — ORIGINAL + STORE particle animation | Every book is a pulsing red star with book-title reveal | Density control, no floating overlays inside reading canvas |
| VR-09 | P0 | Reduced-motion alternative — PLATFORM | Static equivalents to every major animation | Honors accessibility setting; never blocks actions |
| VR-10 | P0 | Physical but short confirmation — CR06, STORE SVG hover/icons | Bookmark, note save, completion, collection move | Feedback only after real operation succeeds |
| VR-11 | P1 | 3D tilt book cover — STORE card UI / CR03 | Featured series and collectible book cards | No tilt on ordinary scrolling; cap angular depth, no motion sickness |
| VR-12 | P1 | Subtle magnifying dock — STORE Apple Dock | Optional Archive/Castle quick launcher | Not shown during immersive reading; small screens use simpler navbar |
| VR-13 | P1 | Tab underline traveling through rail — STORE Navigation Tabs V3/V4/V5 | Library filter: Reading / Paused / Finished | Selected state obvious with motion disabled |
| VR-14 | P1 | Fly-to-shelf animation — STORE Fly To Cart | Adding a book to a collection | Move only visual representation; database action drives state |
| VR-15 | P1 | Mechanical station split-flap nameplate — ORIGINAL | Veil Reader title and archive section header | Can skip intro; never delay first useful content |
| VR-16 | P1 | Shared-element book opening — CR10 / STORE MacBook Scroll | Cover expands into book detail, then reader | Preserve position; support kill/restore; do not simulate page turn |
| VR-17 | P1 | Ambient volumetric fog (tiered) — ORIGINAL | Home/Observatory/Castle, atmospheric distance | Disabled in battery saver, static gradient alternative |
| VR-18 | P1 | Red constellation links — ORIGINAL + Magic UI beams | Every few dozen books form a recognizable constellation | Stable reproducible placement; no network dependency |
| VR-19 | P1 | Observatory galaxy map — CR13 + Filament/SceneView | 3D overview of collections/reading milestones | 3D module decoupled from reader engine; 2D fallback |
| VR-20 | P1 | Castle interactive object hotspot — CR13 | Tap telescope/cabinet/desk to open semantic destinations | Semantics and accessible controls independent of scene |
| VR-21 | P1 | Animated XP ledger — CR09/CR15 | Progress dashboard as alchemical/mechanical instrument | Accurate values; animations from previous-to-new persisted state |
| VR-22 | P1 | 3D hanging seals — CR03 | Ritual rewards, Path badges | Time-limited feedback; avoid screen shake |
| VR-23 | P1 | Brass flipbook settings — CR11 / ORIGINAL | Advanced appearance preview | Instant live preview and one-tap undo |
| VR-24 | P1 | Parchment-to-gold border reveal — STORE SVG icons | Quest unlocked, book finished | Do not obscure text; contrast and color-blind alternatives |
| VR-25 | P1 | Liquid filling reading-time vessel — STORE Liquid OTP | Progression gauge / daily reading ritual | No manipulative streak pressure; exact accessible numbers |
| VR-26 | P1 | Book detail bento composition — CR10 | Series, metadata, highlights, progress | Retrieval/search-first, dense content collapsible |
| VR-27 | P1 | Discovered-script decode — React Bits reference | Lore secrets and constellation labels | Never animate chapter text; instant skip / text accessible |
| VR-28 | P1 | Gentle parallax stacks — STORE MacBook Scroll | Library archive/special collections | Minimal accelerometer dependence; finger scroll primary |
| VR-29 | P1 | Composable motion tokens — Native Compose demos | Shared spring, timing, elevation, opacity behavior | Device-tested token library and automated UI tests |
| VR-30 | P1 | Reading settings focus light — CR07/CR12 | Spotlight to emphasize current slider setting | Actual text contrast unaffected by decoration |
| VR-31 | P2 | Loading particle transmutation — STORE Particle Animation | Brief ambient import/loading states | Never fake progress; no extended blocking animations |
| VR-32 | P2 | Floating interactive cards — CR02/CR03 | Alternate discover/story wall | Must outperform simpler library on find-a-book task |
| VR-33 | P2 | Interactive 3D station room — CR13 | Optional game/world entry | Tiered assets, 3D on explicit entry only |
| VR-34 | P2 | Material cloth/metal badge tactile model — ORIGINAL | Relics, inventory, achievements | Graphic identity consistent; resource budget measured |
| VR-35 | P2 | Liquid/glass micro-button — STORE Glassy Buttons | Rare special action / settings | Don't use everywhere, ensure readable text |
| VR-36 | P2 | Animated login/OTP flow — CR04/05/08/14 | Only if optional sync/account appears later | NO account required for current offline-first baseline |
| VR-37 | X | Payment checkout animations — CR06/STORE | Irrelevant without a store/payments feature | Exclude from core reader scope |
| VR-38 | X | Always-on neon dashboard — CR15 | Very bright game lobby | Reject as default reader surface; fatigue and power risk |
| VR-39 | X | Persistent hovering/wobbling book shelf — CR03 | Continuous decorative motion | Reject; distracts and taxes device |
| VR-40 | X | WebView replacement of core reader with React motion | EPUB/PDF reader | Reject; undermines native mode isolation/performance/QA |

## 3. Concrete reusable technology research (evaluate, do not silently install)

| Candidate | Source | Role | Important caveat |
|---|---|---|---|
| Official Compose Animations | https://developer.android.com/develop/ui/compose/animation/quick-guide | Core Android motion APIs; `Animatable`, spring, `graphicsLayer` | Prefer draw-phase transforms when possible; profile actual devices |
| Compose animation playground | https://github.com/skydoves/compose-animations | Motion prototyping and parameter tuning | Check license before copying exact implementations |
| SceneView | https://github.com/sceneview/sceneview | Compose-native 3D entry backed by Filament | Confirm integration stability and APK size |
| Google Filament | https://github.com/google/filament | 3D physically based rendering | Higher integration complexity; Apache-2.0; asset-size budget |
| Rive Android | https://github.com/rive-app/rive-android | Interactive state-machine animations | Runtime is MIT; check asset/export rights separately |
| Lottie Android | https://github.com/airbnb/lottie-android | Short vector micro-animation assets | Apache-2.0; avoid oversized unbounded playback |
| React Bits | https://github.com/DavidHDev/react-bits | Study animation concepts for web QA lab | MIT + Commons Clause, review redistribution terms; not native Android |
| Magic UI (ideas) | https://magicui.design/ | Particle, beam, dock and hover patterns for web previews | Check current component terms before reuse |
| Animated component skills catalogue | https://github.com/AnayDhawan/Components | Sourcing multiple web libraries for QA prototype | Verify each underlying component's distinct license |
| Animate UI | https://github.com/imskyleen/animate-ui | Alternative accessible web motion primitives | MIT repository; verify individual dependencies |

## 4. Design-system and performance contracts

1. **Reader isolation:** every animation resides in library, world, settings, or chrome modules; never hook heavy rendering into `PAPER CURL` / `SLIDE` or contaminate mode ownership. Kill/restore and DRM/storage behavior remain unaffected.
2. **No hard-coded source imports:** creator sells many React assets. Their marketplace list doesn't grant permission to copy. If direct code is needed, obtain it through authorized purchase with explicit license review and only port eligible portions.
3. **Performance gates:** prototype should feel instantaneous. Target 60fps for ordinary UI on test devices, monitor p95 frame duration, UI/render thread jank, memory, battery use, launch/import latency. These are **proposed gates**, not already achieved metrics. Auto-reduce effects on budget devices.
4. **Accessibility:** honor reduced-motion preferences, screen readers, RTL/LTR, large text, and keyboard/focus. Never encode success solely with hue or motion.
5. **Narrative coherence:** brass/gold for action, parchment for text, deep ink for shell, sparse red stars for discovery. Avoid generic cyan neon and dozens of glass panes.
6. **Audio/haptics:** subtle optional click/switch sounds and haptics only for deliberate actions, off by default or governed by controls; no autoplay in reader.
7. **The artifact must be evidence-first:** before adoption require screenshot/video of interactive sample, logs, test notes, issue/PR links, and actual Android test build confirmation.

## 5. Validation backlog — implement only behind flags

### Phase A — cheap, high-value foundations
- Create a small Compose Interaction Lab isolated from the current reader.
- Implement `VR-01`, `VR-02`, `VR-05`, `VR-06` as four visual prototypes.
- Establish animation tokens (`fast = 120–180ms`, `normal = 180–280ms`, ceremonial <= 600ms) as **initial hypotheses** for in-device tuning, not fixed standards.
- Benchmark input-to-response, 60fps target, focus/RTL/reduced motion. Accept only if usability improves.

### Phase B — identity and world
- Build optional `VR-04`, `VR-08`, `VR-15`, `VR-17`, `VR-18` with a low-motion 2D fallback.
- Verify direct book-opening still works without visiting any animation.
- Avoid visual clutter: user sees only one featured magical interaction at a time.

### Phase C — optional expensive 3D
- Experiment with `VR-19` and `VR-20` in a *separate feature module and flag* using SceneView/Filament.
- Compare 3D runtime, memory, package size, heat and battery vs native 2.5D Compose.
- Never merge by visual novelty alone; require stable device testing.

## 6. Links for later re-scan

- Instagram source: https://www.instagram.com/code.xr/
- Catalog and individual source products: https://topmate.io/code_xr
- Public 78-product bundle listing observed: https://topmate.io/code_xr/2263396
- Android official animation performance recommendations: https://developer.android.com/develop/ui/compose/animation/quick-guide
- Native 3D option: https://github.com/sceneview/sceneview

**This is a reusable research backlog, not a claim that any new feature is already built or shipped.**