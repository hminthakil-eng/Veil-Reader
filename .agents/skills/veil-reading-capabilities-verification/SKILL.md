---
name: veil-reading-capabilities-verification
description: Integrate and test Veil Reader EPUB/PDF search, selection, dictionary/translation, TTS and audio playback with capability-aware Readium reuse and honest feature readiness.
---

# EPUB / PDF / Search / TTS Verification

## Reuse first
Inspect current Readium version and supported modules before writing parsers or navigators. Prefer official APIs for Publication.search(), selection, navigation, TTS and PDFium adapter; verify version-specific API and persisted locator migration requirements.
Do not advertise identical EPUB and PDF capabilities when the upstream adapter lacks parity. Unknown/unavailable support must show an honest state, not a dead button.

## Search/translation
Search results: no silently lost hits, deterministic page boundaries, cancellation on session retirement, bounded resource use for pathological books, exact locator and return-to-previous-location semantics, keyboard and screen reader focus.
Selected-text translation: use supported Android intents or explicitly approved provider; show an actionable no-provider state; preserve note/highlight/look-up and existing selection ownership.
No background upload of selected book text or search snippets without an explicit privacy decision.

## TTS
Compare pronunciation/voices and navigation against supported installed speech engines with consent-based cloud voices as a separate decision.
Test play/pause/seek, paragraphs, sleep timer, voice/speed, audio focus, Bluetooth disconnect, incoming call, lifecycle background/resume, screen off, text/highlight sync and correct EPUB location.
Evaluate cross-language and mixed-script behavior; Persian support matters but must not block good English implementation.
Avoid embedding unknown-license neural voice model weights. Measure app size, RAM, battery and latency for proposed engine changes.

## Gate
Acceptance requires unit/integration, physical-device, accessibility and recovery evidence, with format-specific capability statements. Do not mark a foundational API as user-facing completion.
