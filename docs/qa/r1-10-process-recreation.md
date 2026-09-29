# R1.10 Device Process-Recreation Harness

This harness verifies two Android lifecycle behaviors separately. They must not be conflated.

## Why this exists

The Reader route unit test that recreates `VeilAppViewModel` with the same directly-created `SavedStateHandle` is useful for route logic, but it does not prove Android system process-death restoration.

The official Android testing guidance also treats release-candidate/end-to-end verification as device-based. R1.10 therefore needs a device harness.

## Requirements

- Windows PowerShell
- Android platform-tools / `adb` on PATH
- exactly one authorized Android device or emulator
- current Reader RC installed
- an imported EPUB or PDF

The script does not clear app data, change permissions, install packages, or modify the database.

## Scenario A — system-initiated process death

Open the book at a distinctive location, then run:

```powershell
./scripts/qa/verify-reader-process-recreation.ps1 -Scenario system-kill
```

The harness:

1. records the running PID and device metadata;
2. captures Activity/window state;
3. sends HOME so the Activity is stopped and Android has a valid saved-state opportunity;
4. checks that Veil Reader is no longer the resumed Activity;
5. runs `adb shell am kill com.veilreader.app`;
6. verifies the old process disappears;
7. relaunches the launcher Activity;
8. verifies a new process exists;
9. captures dumpsys + logcat evidence.

Expected:

- the reader route is reconstructed;
- the publication is reopened;
- the reading position comes from durable Room-backed locator/progress;
- no stale Navigator or Publication instance is reused.

## Scenario B — user force-stop

Open the book at another distinctive location, then run:

```powershell
./scripts/qa/verify-reader-process-recreation.ps1 -Scenario force-stop
```

Expected:

- transient route restoration is not required;
- manually reopening the same book must still resume at the durable position.

This deliberately verifies durability independently of Android saved-task restoration.

## Evidence

Each run is written under:

```
artifacts/r1-10-process-recreation/<timestamp>-<scenario>/
```

The folder contains:

- device/run metadata;
- Activity/window state before and after termination;
- launch timing output;
- package state;
- logcat;
- `REVIEW.md` with the human acceptance checklist.

Do not commit generated evidence unless it has been scrubbed for device/user-specific information and is intentionally being attached to the QA record.

## R1.10 exit gate

R1.10 is GREEN only after:

- system-kill passes on EPUB;
- system-kill passes on PDF;
- force-stop durability passes on EPUB;
- force-stop durability passes on PDF;
- each run shows a changed process PID;
- no P0/P1 resume defect remains;
- logs/evidence are attached to the verification issue.

No production merge is implied by a harness pass.
