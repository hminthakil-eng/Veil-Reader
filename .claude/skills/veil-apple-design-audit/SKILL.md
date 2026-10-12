---
name: veil-apple-design-audit
description: Review Veil Reader Compose UI, accessibility, contrast, adaptive layout, typography, RTL, and motion using Apple design principles adapted for Android.
---

# Veil Reader: Apple-inspired Native Design Review

## Verified upstream sources, 2026-10-12
- [dickwu/apple-design-skill](https://github.com/dickwu/apple-design-skill/tree/904b0eedc7cc778152f545506075d5bb5219ce77): read SKILL.md, references/hig/accessibility.md, color.md, typography.md, layout.md, plus relevant per-screen references. Commit 904b0eedc7cc778152f545506075d5bb5219ce77.
- [Elevatormusic/apple-hig](https://github.com/Elevatormusic/apple-hig/tree/e00f1af16c71f4122e7c964563d2f563fd490d7e): read skills/apple-hig/SKILL.md, guidelines/universal.md, needed foundations/patterns, and agents/design-reviewer.md. Commit e00f1af16c71f4122e7c964563d2f563fd490d7e.
- [Official Apple HIG](https://developer.apple.com/design/human-interface-guidelines/) is the source to verify updates.

This is a native project skill referencing two independent tools; it does NOT assert global plugin installation. No copied HIG reference pages, SF fonts/symbols, Apple artwork, or remote executable scripts. Apple's binary asset license is not granted by the second repo's MIT license.

## Authority order
1. Android UX, Material 3/Compose semantics, TalkBack, native back and Android accessibility prevail over iOS-specific chrome.
2. Veil Reader MASTER and page overrides + current Compose theme are canonical; preserve the archival/Grayfog identity and independent publication typography.
3. Adapt Apple principles: purpose, agency, responsibility, familiarity, flexibility, simplicity, craft, delight; also hierarchy, harmony, consistency.
4. No iOS tab-bar cloning, no glass over publication text, no intrusive progress animation.

## Review and implement
1. Name one user flow and inspect *actual* Compose code/screenshots/measurements, noting which artifacts are unavailable.
2. Read the reference skills and only 3-6 pertinent guideline files. Cite exact path/heading for externally sourced findings, or label design judgment.
3. Prioritize: Critical = readability, semantic label, contrast, 48dp native touch targets, RTL, 200% font scaling, TalkBack and reduced motion; High = predictable navigation/back/gesture ownership, empty/offline/error states; Medium = hierarchy, token-based spacing, visual craft and restrained animation.
4. Calculate WCAG ratio on final composited colors: normal text 4.5:1, large text/non-text 3:1. Prefer opaque functional chrome when real EPUB/PDF background is unpredictable.
5. Fix the smallest native Compose component. Preserve Readium selection, locator, PDF/EPUB capability, Paper/Slide/Paged/Scroll separation, lifecycle durability and release gates.
6. Verify relevant tests and record the *actual* result: python3 tools/check_veil_ui_contrast.py; Android unit/lint/UI tests; light/dark/high contrast, compact/tablet, Persian RTL/English LTR, 200% text, TalkBack, reduced motion, gesture cancellation and restart. Mark unrun tests UNVERIFIED, never PASS.

## Report format
Scope + source SHA; evidence/limits; Critical/High/Medium findings each with Where, Why (citation), Native fix and verification; what works; QA matrix; PR and unresolved physical-device gate.

## Applied example
ReaderAccessDock light accent was #8A6630 over #F0E4CC (~4.14:1). The new #795723 measures ~5.21:1 and chrome is opaque over book pixels. The source check in tools/check_veil_ui_contrast.py catches regression but does not certify accessibility app-wide.
