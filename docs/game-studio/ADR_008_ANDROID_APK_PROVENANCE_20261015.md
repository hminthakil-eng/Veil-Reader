# ADR-008 — Android APK Artifact Provenance and Device-Test Boundary

**Accepted engineering policy 2026-10-15.** This ADR supplements ADR-007 of PR #539, and does not authorize a public game release.

1. **Build once from pinned Godot sources:** Godot 4.7.2 debug-only Android export uses exact release-matching export templates with verified official SHA-256. The project enables ETC2/ASTC Android import, uses a deliberately original *temporary QA icon*, and uses JDK17/installed Android SDK.
2. **Signed artifact, separate identity:** package `org.eyadstudio.islandbookshop.qa` cannot overwrite Veil Reader. APK size 57,012,198 bytes, final SHA-256 `D4D189BAE43C38509A1EA3A97DEC27AD831B88C3CD6B8F93E6B2D577ADBCC10D`; signature v2/v3 valid with stock Godot debug signer. No final release signing work has been approved.
3. **No unapproved permissions:** final `aapt` audit rejects Internet, external file storage, camera, microphone, and precise location. These checks occur on APK bytes, not only preset declarations. Audit architecture `arm64-v8a` + `x86_64` and minSdk24.
4. **Protect the Reader work environment:** `emulator-5554` is offline and associated with Reader QA, so do not kill, reset ADB server, wipe it, or install game package there. Use only newly isolated `IslandBookshopQA_API35` on port5584 for installation.
5. **Device-test RED is a real blocker:** Two isolated emulator boot attempts failed before Android's boot-completed property. The Windows host reported `netsimd.exe` crashes (0xc000001d) and low available RAM. This is a **host/emulator issue hypothesis**, not proven game defect, and **not** proof that APK runs.
6. **Do not confuse QA APK with premium release:** the game still contains basic pixel placeholder scenes and an action deck. Touch/input ergonomics, text shaping, screen insets, on-device persistence and accessibility remain unverified and must be played, recorded and iterated before acceptance.
7. **Non-public artifact:** do not publish binary as public GitHub release. Download link can be provided only after a private/approved distribution path exists and the file is verified in that environment.
8. **Next:** a resource-appropriate isolated device, adb install/launch/logcat/screenshot QA, app lifecycle and Persian typography checks, then story content deepening and broader town economy. Keep original user-book content inaccessible to the game.

Evidence: `qa/evidence/ANDROID_APK_EXPORT_20261015.md`. This is consistent with the offline-first user goal and with the equal-peer Gemini desktop critique (earlier consultation in Flash-Lite mode; **no new Gemini 4 Argon validation of this APK claimed**).
