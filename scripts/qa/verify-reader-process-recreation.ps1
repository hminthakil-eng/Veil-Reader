param(
    [ValidateSet("system-kill", "force-stop")]
    [string]$Scenario = "system-kill",

    [ValidatePattern('^[A-Za-z0-9_.]+$')]
    [string]$Package = "com.veilreader.app",

    [string]$Component = "com.veilreader.app/.MainActivity",

    [string]$OutputRoot = "artifacts/r1-10-process-recreation",

    [string]$AdbCommand = "adb",

    [switch]$FunctionsOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-Adb {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    $output = & $AdbCommand @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "$AdbCommand $($Arguments -join ' ') failed with exit code $LASTEXITCODE :: $output"
    }
    return @($output)
}

function Get-AdbDeviceState {
    return ((Invoke-Adb -Arguments @("get-state")) | Out-String).Trim()
}

function Parse-SinglePid {
    param([AllowEmptyString()][string]$Text)

    $trimmed = $Text.Trim()
    if ([string]::IsNullOrWhiteSpace($trimmed)) {
        return ""
    }

    $tokens = @($trimmed -split '\s+' | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ($tokens.Count -ne 1 -or $tokens[0] -notmatch '^\d+$') {
        throw "Ambiguous or malformed pidof result: '$trimmed'."
    }
    return $tokens[0]
}

function Get-AppPid {
    $state = Get-AdbDeviceState
    if ($state -ne "device") {
        throw "ADB transport is not healthy; expected state 'device', got '$state'."
    }

    $output = Invoke-Adb -Arguments @("shell", "sh", "-c", "pidof $Package || true")
    return Parse-SinglePid -Text (($output | Out-String).Trim())
}

function Wait-ForNoPid {
    param([int]$TimeoutSeconds = 10)

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $pidValue = Get-AppPid
        if ([string]::IsNullOrWhiteSpace($pidValue)) {
            return
        }
        Start-Sleep -Milliseconds 250
    } while ((Get-Date) -lt $deadline)

    throw "Process for $Package is still alive after $TimeoutSeconds seconds (pid=$pidValue)."
}

function Wait-ForPid {
    param([int]$TimeoutSeconds = 10)

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $pidValue = Get-AppPid
        if (-not [string]::IsNullOrWhiteSpace($pidValue)) {
            return $pidValue
        }
        Start-Sleep -Milliseconds 250
    } while ((Get-Date) -lt $deadline)

    throw "Process for $Package did not appear after $TimeoutSeconds seconds."
}

function Get-AppActivityBlock {
    param([Parameter(Mandatory = $true)][string]$ActivitiesText)

    $escapedPackage = [regex]::Escape($Package)
    $activityName = (($Component -split '/', 2)[1]).TrimStart('.')
    $escapedActivity = [regex]::Escape($activityName)
    $pattern = "(?ms)^\s*\* Hist\s+#\d+:\s+ActivityRecord\{[^\r\n]*\s$escapedPackage/(?:\.)?$escapedActivity\b[^\r\n]*\}.*?(?=^\s*\* Hist\s+#\d+:|^\s*\* Task\{|\z)"
    $match = [regex]::Match($ActivitiesText, $pattern)
    if (-not $match.Success) {
        return $null
    }
    return $match.Value
}

function Get-AppActivityState {
    param([Parameter(Mandatory = $true)][string]$ActivitiesText)

    $block = Get-AppActivityBlock -ActivitiesText $ActivitiesText
    if ([string]::IsNullOrWhiteSpace($block)) {
        return $null
    }

    $stateMatch = [regex]::Match($block, '(?m)^\s*state=([A-Z_]+)\b')
    if (-not $stateMatch.Success) {
        return $null
    }
    return $stateMatch.Groups[1].Value
}

function Wait-ForActivityState {
    param(
        [Parameter(Mandatory = $true)][string]$ExpectedState,
        [int]$TimeoutSeconds = 10,
        [string]$OutputPath = ""
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastActivities = ""
    $state = $null
    do {
        $lastActivities = (Invoke-Adb -Arguments @("shell", "dumpsys", "activity", "activities") | Out-String)
        $state = Get-AppActivityState -ActivitiesText $lastActivities
        if ($state -eq $ExpectedState) {
            if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
                $lastActivities | Set-Content -Path $OutputPath -Encoding UTF8
            }
            return $lastActivities
        }
        Start-Sleep -Milliseconds 250
    } while ((Get-Date) -lt $deadline)

    if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
        $lastActivities | Set-Content -Path $OutputPath -Encoding UTF8
    }
    throw "Veil Reader did not reach Activity state $ExpectedState within $TimeoutSeconds seconds (last parsed state='$state')."
}

function Wait-ForAppResumed {
    param([int]$TimeoutSeconds = 15)

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $activities = (Invoke-Adb -Arguments @("shell", "dumpsys", "activity", "activities") | Out-String)
        $resumedLine = ($activities -split '\r?\n' | Where-Object {
            $_ -match "mResumedActivity|topResumedActivity" -and $_ -match [regex]::Escape($Package)
        } | Select-Object -First 1)
        if ($resumedLine) {
            return
        }
        Start-Sleep -Milliseconds 250
    } while ((Get-Date) -lt $deadline)

    throw "Veil Reader did not become the resumed Activity within $TimeoutSeconds seconds."
}

function Assert-LauncherRootTaskFromText {
    param([Parameter(Mandatory = $true)][string]$ActivitiesText)

    $escapedPackage = [regex]::Escape($Package)
    $taskMatch = [regex]::Match(
        $ActivitiesText,
        "(?ms)^\s*\* Task\{[^\r\n]*A=\d+:$escapedPackage[^\r\n]*\r?\n.*?(?=^\s*\* Task\{|\z)"
    )
    if (-not $taskMatch.Success) {
        throw "Could not locate the Veil Reader task in dumpsys activity output."
    }

    $taskBlock = $taskMatch.Value
    $taskHeader = ($taskBlock -split '\r?\n' | Select-Object -First 1)
    if (
        $taskHeader -notmatch '\bsz=1\b' -or
        $taskBlock -notmatch 'rootOfTask=true' -or
        $taskBlock -notmatch 'act=android.intent.action.MAIN' -or
        $taskBlock -notmatch 'cat=\[android.intent.category.LAUNCHER\]'
    ) {
        throw "Veil Reader is not a single launcher-root Activity. Task header: $taskHeader"
    }
}

function Assert-LauncherRootTask {
    $activities = (Invoke-Adb -Arguments @("shell", "dumpsys", "activity", "activities") | Out-String)
    Assert-LauncherRootTaskFromText -ActivitiesText $activities
}

function Save-AdbText {
    param(
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$Path
    )

    Invoke-Adb -Arguments $Arguments | Set-Content -Path $Path -Encoding UTF8
}

function Save-BestEffortFailureEvidence {
    param([Parameter(Mandatory = $true)]$Failure)

    if ([string]::IsNullOrWhiteSpace($script:RunDir) -or -not (Test-Path $script:RunDir)) {
        return
    }

    $failureInfo = [ordered]@{
        status = "FAILED"
        scenario = $Scenario
        timestamp = (Get-Date).ToString("o")
        message = $Failure.Exception.Message
    }
    $failureInfo | ConvertTo-Json -Depth 4 | Set-Content -Path (Join-Path $script:RunDir "failure.json") -Encoding UTF8

    try {
        Save-AdbText -Arguments @("shell", "dumpsys", "activity", "activities") -Path (Join-Path $script:RunDir "99-failure-activities.txt")
    } catch {
        $_ | Out-String | Set-Content -Path (Join-Path $script:RunDir "99-failure-activities-error.txt") -Encoding UTF8
    }
    try {
        Save-AdbText -Arguments @("logcat", "-d", "-v", "threadtime") -Path (Join-Path $script:RunDir "99-failure-logcat.txt")
    } catch {
        $_ | Out-String | Set-Content -Path (Join-Path $script:RunDir "99-failure-logcat-error.txt") -Encoding UTF8
    }
}

if ($FunctionsOnly) {
    return
}

$script:RunDir = $null
trap {
    $failure = $_
    Save-BestEffortFailureEvidence -Failure $failure
    [Console]::Error.WriteLine($failure.ToString())
    exit 1
}

$state = Get-AdbDeviceState
if ($state -ne "device") {
    throw "Expected one authorized adb device, got state '$state'."
}

$serial = ((Invoke-Adb -Arguments @("get-serialno")) | Out-String).Trim()
if ([string]::IsNullOrWhiteSpace($serial) -or $serial -eq "unknown") {
    throw "Could not determine adb device serial."
}

Assert-LauncherRootTask

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$runDir = Join-Path $OutputRoot "$timestamp-$Scenario"
New-Item -ItemType Directory -Force -Path $runDir | Out-Null
$script:RunDir = $runDir

Write-Host ""
Write-Host "R1.10 deterministic process-recreation verification"
Write-Host "Scenario : $Scenario"
Write-Host "Device   : $serial"
Write-Host "Package  : $Package"
Write-Host ""
Write-Host "PRECONDITION:"
Write-Host "1. Veil Reader is open in an imported EPUB or PDF."
Write-Host "2. Navigate to a distinctive, easy-to-recognize location."
Write-Host "3. Wait until the final navigation has visibly settled."
Write-Host ""

$marker = Read-Host "Enter a short locator marker (example: EPUB chapter 7 paragraph start / PDF page 42)"
if ([string]::IsNullOrWhiteSpace($marker)) {
    throw "A locator marker is required so the evidence can be reviewed deterministically."
}

Invoke-Adb -Arguments @("logcat", "-c") | Out-Null

$deviceInfo = [ordered]@{
    status = "RUNNING"
    scenario = $Scenario
    timestamp = $timestamp
    serial = $serial
    model = ((Invoke-Adb -Arguments @("shell", "getprop", "ro.product.model")) | Out-String).Trim()
    androidRelease = ((Invoke-Adb -Arguments @("shell", "getprop", "ro.build.version.release")) | Out-String).Trim()
    sdk = ((Invoke-Adb -Arguments @("shell", "getprop", "ro.build.version.sdk")) | Out-String).Trim()
    package = $Package
    component = $Component
    locatorMarker = $marker
}
$deviceInfo | ConvertTo-Json -Depth 4 | Set-Content -Path (Join-Path $runDir "run.json") -Encoding UTF8

$beforePid = Get-AppPid
if ([string]::IsNullOrWhiteSpace($beforePid)) {
    throw "Veil Reader is not running. Open the reader at the marked location before starting the script."
}
$deviceInfo.beforePid = $beforePid

Save-AdbText -Arguments @("shell", "dumpsys", "activity", "activities") -Path (Join-Path $runDir "01-before-activities.txt")
Save-AdbText -Arguments @("shell", "dumpsys", "activity", "top") -Path (Join-Path $runDir "01-before-top.txt")
Save-AdbText -Arguments @("shell", "dumpsys", "window", "windows") -Path (Join-Path $runDir "01-before-windows.txt")

Invoke-Adb -Arguments @("shell", "input", "keyevent", "KEYCODE_HOME") | Out-Null
$afterHomePath = Join-Path $runDir "02-after-home-activities.txt"
Wait-ForActivityState -ExpectedState "STOPPED" -TimeoutSeconds 10 -OutputPath $afterHomePath | Out-Null

if ($Scenario -eq "system-kill") {
    Invoke-Adb -Arguments @("shell", "am", "kill", $Package) | Out-Null
} else {
    Invoke-Adb -Arguments @("shell", "am", "force-stop", $Package) | Out-Null
}

Wait-ForNoPid -TimeoutSeconds 10

Save-AdbText -Arguments @("shell", "dumpsys", "activity", "activities") -Path (Join-Path $runDir "03-after-termination-activities.txt")
Save-AdbText -Arguments @("shell", "dumpsys", "package", $Package) -Path (Join-Path $runDir "03-after-termination-package.txt")

if ($Scenario -eq "system-kill") {
    $launchOutput = Invoke-Adb -Arguments @(
        "shell", "am", "start", "-W",
        "-a", "android.intent.action.MAIN",
        "-c", "android.intent.category.LAUNCHER",
        "-f", "0x10200000",
        "-n", $Component
    )
    $launchOutput | Set-Content -Path (Join-Path $runDir "04-launch.txt") -Encoding UTF8
    $afterPid = Wait-ForPid -TimeoutSeconds 10
    Wait-ForAppResumed -TimeoutSeconds 15
    Read-Host "System-kill relaunch is resumed. Inspect the restored book/position; press Enter when stable to capture final evidence" | Out-Null
} else {
    "Force-stop intentionally requires a real manual launcher reopen." | Set-Content -Path (Join-Path $runDir "04-launch.txt") -Encoding UTF8
    Read-Host "Force-stop complete. Reopen Veil Reader from the launcher, manually open the SAME book, wait for the durable position, then press Enter" | Out-Null
    $afterPid = Wait-ForPid -TimeoutSeconds 30
    Wait-ForAppResumed -TimeoutSeconds 15
}

$deviceInfo.afterPid = $afterPid
$deviceInfo.pidChanged = ($beforePid -ne $afterPid)

if ($beforePid -eq $afterPid) {
    throw "The process PID did not change across termination/relaunch; this run is not valid process-recreation evidence."
}

Save-AdbText -Arguments @("shell", "dumpsys", "activity", "activities") -Path (Join-Path $runDir "05-after-relaunch-activities.txt")
Save-AdbText -Arguments @("shell", "dumpsys", "activity", "top") -Path (Join-Path $runDir "05-after-relaunch-top.txt")
Save-AdbText -Arguments @("shell", "dumpsys", "window", "windows") -Path (Join-Path $runDir "05-after-relaunch-windows.txt")
Save-AdbText -Arguments @("logcat", "-d", "-v", "threadtime") -Path (Join-Path $runDir "06-logcat.txt")

$deviceInfo.status = "CAPTURED"
$deviceInfo | ConvertTo-Json -Depth 4 | Set-Content -Path (Join-Path $runDir "run.json") -Encoding UTF8

$review = @"
# R1.10 run review

Scenario: $Scenario
Locator marker before termination: $marker
PID before: $beforePid
PID after: $afterPid
PID changed: $($beforePid -ne $afterPid)

## Machine-verified preconditions

- [x] ADB transport was healthy for PID checks.
- [x] Veil Reader Activity positively reached STOPPED after HOME.
- [x] Process disappeared after termination.
- [x] A different process PID appeared after relaunch/manual reopen.
- [x] Veil Reader became the resumed Activity before final capture.

## Human verification

- [ ] App launched without crash.
- [ ] Scenario semantics matched the expected route behavior.
- [ ] EPUB/PDF content reopened as expected for this scenario.
- [ ] Durable locator/progress matched the marker closely enough to satisfy the acceptance threshold.
- [ ] No stale Navigator/Publication behavior was observed.
- [ ] Evidence files and logcat were retained.

### Expected semantics

system-kill:
- active reader route SHOULD be reconstructed from Android saved state;
- durable locator/progress SHOULD come from persistent storage.

force-stop:
- active reader route is NOT required to restore;
- after manually reopening the same book, durable locator/progress MUST still be correct.
"@
$review | Set-Content -Path (Join-Path $runDir "REVIEW.md") -Encoding UTF8

Write-Host ""
Write-Host "Capture complete: $runDir"
Write-Host "PID before=$beforePid after=$afterPid changed=$($beforePid -ne $afterPid)"
Write-Host "Complete REVIEW.md after inspecting the relaunched reader."
