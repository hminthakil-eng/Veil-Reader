# Manga renderer evidence collector

Use `tools/capture-manga-renderer-evidence.ps1` during issue #177 physical renderer testing.

Recommended sequence:

```powershell
powershell -ExecutionPolicy Bypass -File tools\capture-manga-renderer-evidence.ps1 -Phase baseline
# perform the giant-image pager stress
powershell -ExecutionPolicy Bypass -File tools\capture-manga-renderer-evidence.ps1 -Phase peak
# leave the stress chapter and allow memory to settle
powershell -ExecutionPolicy Bypass -File tools\capture-manga-renderer-evidence.ps1 -Phase settled
```

When multiple Android devices are online, add `-Serial <adb-serial>`.

Each capture records:
- exact git commit SHA
- device model/API/security patch/fingerprint
- screen size/density
- app PID
- `dumpsys meminfo com.veilreader.app`
- `dumpsys gfxinfo com.veilreader.app`
- matching process-state excerpt
- filtered logcat for Veil, LMK, OOM and fatal native/Java failures

Evidence is written under ignored `build/manga-renderer-evidence/`.

The script is measurement-only: it does not install/uninstall the app, clear app data, change renderer settings or automate gestures. Pair it with the manual matrix in issue #177.

Interpretation rule: a host build is not a device pass. A renderer only advances when memory does not cause app death, PSS/native heap settles materially after stress, gestures remain correct and the exact tested SHA is recorded.