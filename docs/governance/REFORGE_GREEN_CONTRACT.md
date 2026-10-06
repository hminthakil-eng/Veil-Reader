# Veil Reader — Reforge GREEN Contract

Date: 2026-10-05  
Owner: Veil Reader Grand Forge + Arena  
Applies to: every user-visible feature, UI surface, data flow, Reader mode, import/export path, background task and release candidate.

## Why this exists

Veil accumulated many substantial capabilities, but code existence and passing automation were repeatedly mistaken for product completeness.

From Reforge onward:

**Code presence is not GREEN.**
**Unit-test success is not GREEN.**
**CI success is not GREEN.**
**A good screenshot is not GREEN.**

GREEN means the capability is usable, truthful, resilient, accessible, visually finished and proven at the level appropriate to its risk.

## Statuses

### RED
Known broken, dangerous, misleading, data-losing, unusable, or product-owner rejected.

### YELLOW
Meaningful implementation exists but one or more required gates are missing.

### GREEN-SOURCE
Source and automated correctness gates pass. This is an engineering milestone only.

### GREEN-DEVICE
Relevant real-device interaction/rendering gates pass.

### GREEN-PRODUCT
Product owner can use the capability as intended and all applicable quality gates pass.

Only **GREEN-PRODUCT** means “done” for user-facing Reforge work.

## Universal quality gates

A user-visible capability must satisfy every applicable gate:

### 1. Functional completeness
- Primary happy path works end-to-end.
- Secondary actions work.
- No dead controls.
- No fake controls.
- No placeholder behavior represented as finished.
- Cancel/back/retry semantics are explicit.

### 2. State and durability
- Rotation/window resize behavior is defined.
- Background/foreground behavior is defined.
- Process recreation is tested where state matters.
- Critical writes are durable before ownership is released.
- Old async results cannot mutate a newer session.

### 3. Failure states
- Loading is designed.
- Empty state is designed.
- Error state is designed.
- Retry path exists where recovery is possible.
- Irrecoverable failure gives a truthful next action.
- Cancellation never becomes a generic failure.

### 4. UI/UX finish
- Primary action is visually obvious.
- Visual hierarchy matches task priority.
- Spacing is intentional.
- No unnecessary decoration delays the task.
- Disabled/pressed/selected/focused/loading/error states are designed.
- Screen remains coherent with neighboring realms.

### 5. Accessibility
- Essential touch targets are >=48dp.
- TalkBack labels/roles/state are correct.
- Focus order is correct.
- Switch/keyboard access is supported where applicable.
- Meaning is not color-only.
- High Contrast works.
- Reduced Motion has an equivalent experience.

### 6. Text and localization
- English works.
- Persian/RTL works.
- Mixed-script content works.
- 200% text preserves all essential function.
- Text is not shrunk solely to repair layout.
- User-visible strings are localized unless intentionally technical/debug-only.

### 7. Adaptive behavior
Test applicable combinations:
- compact portrait
- landscape / short height
- tablet / expanded width
- foldable/resizable window
- keyboard/IME open

### 8. Performance
- No obvious jank in the primary interaction.
- Memory is bounded.
- Large images are decoded appropriately.
- Expensive work does not block the main thread.
- Benchmarks exist for performance-critical flows.

### 9. Security/privacy
- Untrusted files/archives are bounded and validated.
- No secrets are committed.
- Network behavior is explicit and optional where product policy requires.
- Backup/export surfaces warn about private data where relevant.

### 10. Evidence
Every GREEN claim states:
- exact executable SHA
- automated runs
- relevant screenshots/captures
- device/OS when physical proof matters
- known limitations

## Special gates by subsystem

### Reader / Paper / Slide
Cannot become GREEN-PRODUCT without:
- real finger-driven device proof
- first-turn proof
- cancel/reverse proof
- destination-flash check
- rotation/background/context recreation
- 60/90/120Hz where hardware permits
- repeated-turn memory stability

### PDF
Cannot become GREEN-PRODUCT solely from unit tests.
Real PDFs must cover:
- text
- scanned/image-heavy
- links
- long documents
- zoom
- paginated/continuous
- RTL where applicable

### Sync
Cannot become GREEN-PRODUCT without:
- offline-first operation
- retry/backoff
- duplicate delivery safety
- conflict behavior
- deletion/tombstone behavior
- multi-device race tests

### Import/restore
Cannot become GREEN-PRODUCT without:
- interrupted import
- low-storage behavior
- malformed input
- duplicate input
- rollback
- restored data integrity

### UI/visual changes
Cannot become GREEN-PRODUCT from source inspection alone.
At minimum:
- actual rendered capture
- English + Persian
- 100% + 200% text
- normal + High Contrast where relevant

## PR rule

Every PR must state:
1. what user problem it solves;
2. current status before the PR;
3. which gates it changes;
4. exact validation evidence;
5. what remains YELLOW;
6. whether it changes persistence, Reader ownership, security, or public release behavior.

If any applicable gate is not proven, the PR must not call the feature GREEN-PRODUCT.

## Rejection rules

Reject a change if it:
- adds a second ownership path for the same interaction;
- hides a broken backend behind polished UI;
- adds visual spectacle at the cost of retrieval/reading;
- reduces accessibility to preserve composition;
- adds an unsupported setting;
- rewrites a stable core without evidence;
- removes tests to make a gate pass;
- labels emulator/source evidence as physical-device proof;
- silently changes local-first behavior.

## Completion rule

A feature leaves the Reforge backlog only when:
- its applicable Definition of Done is complete,
- no unresolved RED issue remains,
- any remaining limitation is explicitly accepted as product policy,
- and the resulting user experience is at least commercially usable.

The target is not more code. The target is a feature that earns its place.
