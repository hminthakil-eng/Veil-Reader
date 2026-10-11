# Veil Reader — Sherpa Android engine cancellation patch (experimental)

**Status:** UPSTREAM PATCH PREPARED — **not installed, built, physically tested, or promoted**.

The official `k2-fsa/sherpa-onnx` Android TextToSpeechService example, upstream path
`android/SherpaOnnxTtsEngine/app/src/main/java/com/k2fsa/sherpa/onnx/tts/engine/TtsService.kt`,
currently implements `override fun onStop() {}` and always returns `1` from the
streaming synthesis callback without checking `SynthesisCallback.audioAvailable`.

This can continue expensive native inference after an explicit system stop. It is a
**potential cause of resource leaks and stop noncompliance**; it is **not proof** that
audible audio must continue, because Android's framework owns the output buffer.

## What this patch changes

Apply both patches in order:

1. `0001-cancellable-synthesis-and-pause.patch` (stop propagation)
2. `0002-remove-plaintext-narration-logs.patch` (privacy: no raw book text in Logcat)
3. `0003-kokoro-multivoice-identity.patch` (11 independently addressable voice IDs, exact per-request speaker selection; **Kokoro only**)

The first patch:
- Increments a thread-visible `AtomicLong` cancellation generation in `onStop()`.
- Captures the generation per synthesis request and checks cancellation before each PCM
  conversion and before each piece of `audioAvailable`; returns `0` to the sherpa
  generation callback when stop is requested.
- Stops native callback streaming when `audioAvailable()` returns an error, and rejects
  illegal `maxBufferSize <= 0` (prevents a zero-length infinite write loop).
- Sends `done()` only on successful, uncanceled completion; reports real exceptions
  with `error()`, without emitting an out-of-order `done()` after Pause.
- Retains the engine's original package name, voice and synthesis settings.

## Apply against the verified upstream file only

Source project: https://github.com/k2-fsa/sherpa-onnx

Pinned upstream **commit:** `9db1af1871ed78cee9357c23898e5a7d8702d21a`.
The [independent CI audit](../../.github/workflows/sherpa-tts-stop-patch-audit.yml) checks this commit's exact file blob, applies the patch and runs `git diff --check`. It does not compile a patched engine or run actual audio tests.

Exact original upstream **Git blob SHA-1**:
`05ef5d3d6d553646bec8cdcf4f9240d2ccda7f60`.
This is a file hash, **not** a commit or a proof of a reviewed native binary.
Always pin the full source commit separately when creating a distributable build.

```bash
# In a local checkout of k2-fsa/sherpa-onnx; do not patch Veil Reader itself.
SHERPA_ROOT=/absolute/path/to/sherpa-onnx
TARGET=android/SherpaOnnxTtsEngine/app/src/main/java/com/k2fsa/sherpa/onnx/tts/engine/TtsService.kt
git -C "$SHERPA_ROOT" hash-object "$TARGET"
# Must be: 05ef5d3d6d553646bec8cdcf4f9240d2ccda7f60

git -C "$SHERPA_ROOT" apply --check \
  /absolute/path/to/Veil-Reader/tools/sherpa-onnx-engine-patch/0001-cancellable-synthesis-and-pause.patch
git -C "$SHERPA_ROOT" apply \
  /absolute/path/to/Veil-Reader/tools/sherpa-onnx-engine-patch/0001-cancellable-synthesis-and-pause.patch
git -C "$SHERPA_ROOT" apply --check \
  /absolute/path/to/Veil-Reader/tools/sherpa-onnx-engine-patch/0002-remove-plaintext-narration-logs.patch
git -C "$SHERPA_ROOT" apply \
  /absolute/path/to/Veil-Reader/tools/sherpa-onnx-engine-patch/0002-remove-plaintext-narration-logs.patch
git -C "$SHERPA_ROOT" apply --check \
  /absolute/path/to/Veil-Reader/tools/sherpa-onnx-engine-patch/0003-kokoro-multivoice-identity.patch
git -C "$SHERPA_ROOT" apply \
  /absolute/path/to/Veil-Reader/tools/sherpa-onnx-engine-patch/0003-kokoro-multivoice-identity.patch
```

### True narrator selection (Kokoro only)

Upstream `TtsService` used a global `TtsEngine.speakerId` for every synthesis
request, even when the Android client selected a different voice. The third
patch exposes one Android `Voice` per native speaker embedding:
`kokoro-en-v0_19-0` through `kokoro-en-v0_19-10` (actual count from
`numSpeakers()`). Android clients can then use `TextToSpeech.setVoice()`;
`SynthesisRequest.voiceName` selects the exact speaker ID for that bounded
synthesis request, without changing a process-global mutable narrator.

Never apply this **Kokoro-only** patch to a Persian Piper/Matcha engine:
those require their own language/voice catalogs. A verified native `numSpeakers()`
return and audible 11-voice comparison are still required to assert real
multispeaker runtime behavior. Do not confuse a modeled list with audio proof.

**Do not silently drop patch failures.** If upstream changes, re-review the service
and its JNI worker callback threading. Do not ship a modified binary using this patch
alone; AndroidX, JNI, model license, packaging, ABI and the runtime version must be
reviewed separately.

## Mandatory physical acceptance (issue #484)

1. Android device 60Hz/120Hz with an installed and explicitly configured Kokoro model.
2. Repeated TTS stop while waiting for first audio, while streaming, after output buffer
   has queued frames, during a transient focus loss and while locked.
3. Measure (a) final **audible** time after Pause, (b) native thread/CPU continuing
   synthesis, and (c) whether the Android `TextToSpeech` client actually bound to
   the intended engine; these three assertions are independent.
4. Confirm user-selected voice identity at 20+ sentence/chapter transitions and after
   Pause/Resume; no overlapping voice preview.
5. Device-specific JNI regression: `generateWithConfigAndCallback` can invoke callbacks
   on native threads; upstream #3492 reports a crash in older versions. Verify that
   the native sherpa binary used by the patched engine no longer has this defect.
6. Record exact source/binary SHA-256, native library version, model files and their
   SHA-256, license, device OS/ABI, crash logs and a 30+ minute long reading session.

If any gate fails, keep the neural option **disabled for release**; do not automatically
fall back to another narrator. The user-facing Veil fix for Pause during pending
Readium startup is separate and already lives in draft PR #482. The production
neural gate remains OFF.
