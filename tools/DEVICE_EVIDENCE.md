# Device evidence runner

Use `tools/run-device-evidence.ps1` for the first physical-device RC smoke gate. It reuses `ReaderSafImportInstrumentedTest`; it does not create a second UI-test path.

## Prerequisites

- Android device connected by USB with USB debugging authorized.
- Device unlocked or unlockable during the run.
- Android DocumentsUI available.
- Clean Git worktree so the recorded commit SHA exactly matches the tested build.
- JDK 17, Gradle 9.6 and Android SDK already configured for Veil Reader.

## Run

Single connected device:

```powershell
powershell -ExecutionPolicy Bypass -File tools\run-device-evidence.ps1
```

When more than one Android device is online:

```powershell
powershell -ExecutionPolicy Bypass -File tools\run-device-evidence.ps1 -Serial <adb-serial>
```

## What it does

1. Rejects missing, offline, unauthorized or ambiguous devices.
2. Records commit SHA plus device manufacturer/model, Android/API/security patch, ABI, fingerprint, screen size and density.
3. Pushes `qa/fixtures/veil-smoke.epub` to `Downloads/VeilReaderQa.epub`.
4. Builds the debug app and AndroidTest APK offline.
5. Installs both APKs on only the selected serial. It never uninstalls the user's app/data automatically.
6. Discovers the installed instrumentation runner and executes `ReaderSafImportInstrumentedTest`.
7. Requires a real `OK (1 test)` result and rejects instrumentation failures/crashes.

Evidence is stored under the ignored `build/device-evidence/<timestamp>-<serial>/` directory, including metadata, ADB listing, Gradle output, install output and instrumentation output.

A host build or emulator result must not be reported as physical-device acceptance.
