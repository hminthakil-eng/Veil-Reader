# Arena Product North Star — Veil Reader
Date: 2026-09-30
Branch: `alpha/arena-rebuild-v2`
Owner: Arena / Alpha

## Product ambition

Build Veil Reader as a commercially serious reading product whose value survives comparison with mature ebook readers without becoming a clone.

"Million-dollar product" is a quality and market ambition, not a revenue guarantee. The product must earn that possibility through retention, trust, differentiation, and disciplined execution.

## The three-layer moat

### 1. Reading Core — trust
The user must be able to import a book, open it, read comfortably, return to the exact location, annotate it, and leave safely without thinking about the app.

Non-negotiable:
- EPUB and PDF remain first-class.
- offline-first; reading imported books never depends on an account, network, game, or AI service.
- durable location, highlights, notes, bookmarks, appearance, and history.
- Paged / Scroll is independent from Paper / Slide / None.
- PDF keeps direct manipulation and native-feeling zoom.
- Reader is the quietest realm.
- accessibility, RTL, large type, reduced motion, process death, and rotation are product requirements.

This layer creates trust. Nothing in the World layer may make it less reliable.

### 2. Living Memory — differentiation
Veil must remember the reader's relationship with books, not merely store files.

Core memory primitives:
- reading sessions and active time
- completion cycles and rereads
- factual milestones
- preserved passages and exact passage revisits
- archive depth / Deep Shelf
- Echoes
- Time Capsules
- Memory Atlas / Observatory
- durable discoveries

Rules:
- never invent history.
- distinguish exact passage revisit from generic later book activity.
- once a discovery is historically earned, it does not disappear because a live condition later falls.
- memory surfaces must remain useful even if gamification is disabled.

This layer is the primary defensible product identity.

### 3. World Layer — desire and long-term retention
Threshold, Castle, Path, Treasury, Sanctum and Observatory make long-term use emotionally legible.

Rules:
- the world grows from real reading data.
- reading features are never locked behind ranks.
- progression supports reading; it does not punish missed days or turn books into chores.
- rare effects stay rare.
- discoveries and relics should feel earned through history, not purchased pressure.
- world surfaces may be visually rich; Reader must remain calm.

## The product flywheel

Import a book
→ reading feels materially excellent
→ user preserves a passage / bookmark / note
→ Veil remembers that interaction
→ memory resurfaces meaningfully later
→ Archive and Castle gain historical depth
→ user has a reason to return to both the book and Veil
→ more factual history accumulates
→ the product becomes more valuable to that reader over time.

The flywheel must work locally and offline.

## Premium without "premium paint"

Premium means:
- deterministic continuity
- authored information hierarchy
- strong typography
- excellent empty/loading/error states
- one clear primary action
- low-friction retrieval
- deliberate material and motion
- no generic nested-card soup
- no gratuitous glassmorphism
- no permanent decorative noise
- no raw technical exceptions in user surfaces
- no half-localized realm

Gold/brass, fog, parchment and occult geometry are identity materials, not substitutes for product quality.

## Performance tiers

Veil supports:
- Full
- Balanced
- Essential

The tier may scale:
- fog
- procedural geometry
- particles
- ornament
- ceremonial motion

The tier must never scale down:
- text quality
- reading correctness
- touch targets
- persistence
- accessibility
- search accuracy
- book content
- annotation fidelity

## Commercial architecture

The core reader and local ownership of imported books must remain trustworthy.

Potential paid value should come from optional expansion rather than hostage mechanics:
- advanced cross-device sync when/if a backend exists
- optional cloud backup
- optional premium world/customization packs
- optional advanced AI-assisted reading tools when explicitly enabled
- future platform/ecosystem features

Do not put basic reading, offline access, local notes, location restoration, accessibility, or file ownership behind a recurring paywall.

No monetization implementation is authorized by this document; it only defines product boundaries.

## Retention ethics

Veil should create attachment through accumulated meaning, not anxiety.

Allowed:
- calm milestones
- historical discoveries
- reading continuity
- resurfacing old passages
- collections that gain depth
- voluntary goals
- earned visual identity

Avoid:
- punishment for broken streaks
- fake urgency
- loss of earned discoveries
- manipulative daily nags
- loot-box-like randomness
- progression that blocks reading
- noisy reward explosions in Sanctuary

## Arena admission gate for new features

A proposed feature enters the main product only if it passes at least one value test:

1. Reading — makes reading faster, calmer, clearer, safer or more controllable.
2. Retrieval — helps find the right book, passage, note or location.
3. Memory — creates or reveals truthful historical value.
4. Accessibility — improves reach without degrading other users.
5. World — turns real reading history into meaningful delight.
6. Reliability/performance — reduces failure, latency, battery or jank.

And it must pass all constraints:
- local/offline behavior defined
- empty/error/loading state defined
- RTL/localization strategy defined
- accessibility strategy defined
- persistence ownership defined
- test strategy defined
- no duplicate existing subsystem
- no Reader regression

Otherwise it stays out.

## Must-win surfaces

### Threshold
One action back into reading. Environmental identity and current book feel like one scene.

### Grayfog Archive
Fast retrieval first; memory depth second. Search, filters, sorting, gallery/index/shelves remain practical.

### Book Detail
A narrative destination, not a generic metadata sheet.

### Sanctuary
Publication first. Chrome disappears. Appearance/Notebook/Bookmark remain one gesture away.

### Hidden Archive
Notes, passages, bookmarks, Echoes and Time Capsules share one memory language.

### Observatory
Shows only recorded relationships. No fabricated intelligence.

### Castle / Path / Treasury / Sanctum
Long-term optional world. Reads from durable progression/history. Never owns access to books.

### Settings
Calm progressive disclosure, not a control dump.

## Definition of world-class

A screen is not world-class because it matches a screenshot.

It must survive:
- empty library
- one book
- hundreds/thousands of books
- missing cover
- very long title/author
- Persian/RTL
- large font
- TalkBack
- compact phone
- landscape
- tablet/foldable window
- process recreation
- slow import/open
- storage failure
- reduced motion
- Essential quality tier

## Verification ladder

Status vocabulary:
- DESIGNED
- IMPLEMENTED
- SOURCE-VERIFIED
- TEST-GREEN
- DEVICE-GREEN
- RELEASE-GREEN

Never skip labels.

No feature becomes RELEASE-GREEN without:
1. compile
2. unit tests
3. relevant instrumented tests
4. device interaction proof
5. RTL/accessibility proof where relevant
6. performance/regression evidence
7. package verification for release-critical changes

## Current Arena priorities

1. Complete localization/world vocabulary across all production realms.
2. Eliminate raw/technical user-facing errors.
3. Persist earned discoveries historically.
4. Finish Full/Balanced/Essential performance scaling.
5. Strengthen gamification durability to match Reader/library trust.
6. Expand world/settings/archive regression coverage.
7. Continue visual reconstruction of Book Detail, Hidden Archive, Castle, Path, Observatory and Profile.
8. Build/device verification only after the design rebuild stabilizes.

## Final product rule

Veil Reader should become more valuable the longer a person reads with it.

If a feature does not make reading better, retrieval faster, memory richer, accessibility stronger, reliability higher, or the world more meaningfully connected to real reading history, it does not deserve product complexity.
