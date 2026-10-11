# Veil Reader — Native Neural Listening v1
## Evidence-based architecture decision, 2026-10-11

**Status: PROTOTYPE / PRODUCT RED.** Never claim usable local neural speech because
a source contract or an upstream demo compiles. Keep `LOCAL_NEURAL_TTS` release-OFF
and current system TTS functional until actual Android audio tests pass.

### Decision: In-app neural synthesis, no separate Android TTS provider APK

The current experiment (draft [#482](https://github.com/hminthakil-eng/Veil-Reader/pull/482))
routes `TextToSpeech(..., sherpa engine package)` to a separate installed 338-MB
synthesizer APK. It introduces engine discovery, Android framework fallback
ambiguity, two native process/application installations, external model settings,
mediated Pause, and external app upgrades independent of Veil QA.

Replace this as the **target architecture** with the upstream Apache-2.0
`k2-fsa/sherpa-onnx` JNI/Kotlin runtime **inside Veil**, using downloaded,
content-hashed per-language model packs and our **own PCM player**. No public
release dependency on the current external TTS service.

| Mode | Default install | Per-voice model | Use |
| --- | --- | --- | --- |
| Android System | Built-in, zero weight download | OS owns | Immediate fallback selected by user |
| English premium | Native Kokoro ONNX | Optional, reviewed full-precision reference first | Quality first, benchmark FP16/mixed precision after |
| English light | KittenTTS or well-tested Piper voice | Optional, smaller | Lower-end devices; not quality-equivalent by assumption |
| Persian local | Piper fa_IR or Matcha fa_en | Optional, independently licensed/verified | Validate pronunciation with Persian and bilingual books |
| Future | Supertonic 3 / others | Opt-in R&D | OpenRAIL-M weights need separate downstream compliance review |

**No silent narrator switching or network requests.** A failed selected engine
returns a clear model/unavailable error and offers a user-initiated System TTS
choice; never auto-switch voice in the middle of a paragraph.

### Reuse what Veil already has

- `ReaderTtsNeuralRuntime`: native PCM chunk interface, already includes
  selected voice ID and sample rate; preserve it.
- `ReaderTtsModelStore`, `ReaderTtsModelPackage`,
  `ReaderTtsModelPreparer`, archive extraction: atomic installs, sha256,
  storage budget, safe extraction. Feed only reviewed signed/source-pinned models.
- `ReaderTtsSession` and existing Readium TTS iterator: chapter/EPUB parsing,
  checkpoint, semantic position, explicit Play/Pause, audio focus, background.
- `ReaderTtsMediaPlayer`: Media3 transport and lock-screen compatibility.
- `ReaderTtsPcmEpoch`: explicit native owner/voice lease invalidated at Pause.
- `ReaderTtsInProcessCoordinator`: per-request worker serialization, callback
  epoch check, PCM array detachment and *audible drain* before completion.

### Required final pipeline

```text
Readium selected semantic segment & position
           |
     ReaderTtsSession (single owner, focus and checkpoint)
           |
   In-process native sherpa-onnx (JNI/Kotlin)
       locked model ID + exact narrator/speaker ID
           |
   cancellable callback / epoch generation fence
           | bounded PCM queue, never unbounded
     Veil-owned Android AudioTrack (USAGE_MEDIA, CONTENT_TYPE_SPEECH)
           |
    Media3 playback, notifications, headphones, resume
```

For Pause, order matters:
1. Atomically revoke synthesis generation (UI thread; no native owner lock).
2. Immediately pause + flush the owned AudioTrack (drop already-enqueued frames).
3. Reject every stale native callback and signal native synth to exit.
4. Cancel next-sentence prefetch; preserve the current semantic segment/locator.
5. Resume **only** on user/system-authorized Play; keep the same narrator ID
   and position. No delayed PREPARING callback can assert Play.

For end-of-segment, **do not** emit ENDED when inference returns. Drain the
last queued PCM until hardware playback head actually reaches the final frame;
otherwise the end of every segment is audibly truncated.

### Android/JNI integration gates (unimplemented)

1. Package only the Apache-2.0 upstream Kotlin JNI APIs and tested arm64
   binaries inside a dedicated optional Gradle module. Pin upstream commit,
   native shared object SHA-256 and ABI. Check native transitive licenses
   independently. Android target remains minSdk 26.
2. Native load runs after UI ready on worker threads. Avoid touching JNI
   static initializers or loading 300MB model during Reader composition.
3. Prepare a real in-process Kokoro runtime implementing
   `ReaderTtsNeuralRuntime.synthesize`; `onPcmChunk` must be cancelable and
   use a bounded queue. Never call `OfflineTts.free()` while JNI callbacks
   remain active. Use one native instance initially, parallel producers
   only when hardware profiling proves their benefit.
4. Implement production PCM sink with non-blocking short writes, foreground
   MediaSession, audio focus and no audible tail after Pause. Serialize
   close/free after native job completes; prevent device audio route races.
5. Install voice pack only after explicit user consent through existing
   model store. Budget both compressed + expanded storage and native RSS.
   No plaintext EPUB passages in logs, cloud analytics or network.
6. Benchmark on physical midrange + high-end Android devices: first audible
   frame P50/P95/P99, RTF and underrun gaps, stop-to-silence, 30+ minutes
   continuous playback, memory/thermal/battery and all 11 speaker variants.
7. Verify not just English: Persian/RTL and mixed-language paragraphs, large
   `Lord of the Mysteries` and `Circle of Inevitability` EPUB fixtures,
   names/proper nouns and chapter changes.
8. Flip release gate only after passing objective regression metrics plus
   blinded AB listening test against system and another neural engine.

### Borrow validated architectural patterns, not GPL source

[Candela](https://github.com/techempower-org/candela) uses in-process
VoxSherpa inference plus an owned AudioTrack and downloads models on demand.
It documents producer/consumer, PCM caching, buffering and Media3 integration.
Candela and VoxSherpa-TTS license their code under **GPL-3.0**; do not
copy those source files into Veil without a conscious whole-app GPL-3.0
compliance decision. Implement independent playback using
[Apache-2.0 sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) and
the already-existing Veil architecture.

Also review candidate multilingual
[Supertonic 3](https://supertonic3.github.io/) **weights OpenRAIL-M**,
not MIT. Model files require their own licenses and SHA-256, even if the
inference library is Apache-2.0.

### Exact delivery status

**Implemented in this branch:** generation ownership / narrator fence,
malformed-PCM rejection, injected in-process coordinator, per-chunk detached
PCM, serialized synthesis and regression tests for Pause, stale native
callbacks, audible drain, voice locking and concurrent cancellation.

**Not implemented yet:** packaged JNI runtime, user model installer UI,
real `AudioTrack` sink, playable native Kokoro inside Veil, verified natural
voice quality. No APK can be called native-integrated from this PR yet.
The earlier external app Kokoro APKs remain experimental comparison tools.

**Work definition of done:** user installs only Veil Reader, opts into the
chosen language voice pack once, starts an EPUB, immediately hears the same
voice across segments, pauses instantly including during first-frame
generation and on lock-screen, and resumes the correct sentence with no
speaker changes or clipped tails. All backed by exact-head CI and device
audio evidence.
