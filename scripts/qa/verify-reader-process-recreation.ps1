param(
    [ValidateSet("system-kill", "force-stop")]
    [string]$Scenario = "system-kill",

    [string]$Package = "com.veilreader.app",

    [string]$Component = "com.veilreader.app/.MainActivity",

    [string]$OutputRoot = "artifacts/r1-10-process-recreation"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-Adb {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    $output = & adb @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "adb $($Arguments -join ' ') failed with exit code $LASTEXITCODE`n$output"
    }
    return @($output)
}

function Get-AppPid {
    $output = & adb shell pidof $Package 2>$null
    if ($LASTEXITCODE -ne 0) {
        return ""
    }
    return ($output | Out-String).Trim()
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

function Save-AdbText {
    param(
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$Path
    )

    Invoke-Adb -Arguments $Arguments | Set-Content -Path $Path -Encoding UTF8
}

$state = (Invoke-Adb -Arguments @("get-state") | Out-String).Trim()
if ($state -ne "device") {
    throw "Expected one authorized adb device, got state '$state'."
}

$serial = (& adb get-serialno | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($serial)) {
    throw "Could not determine adb device serial."
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$runDir = Join-Path $OutputRoot "$timestamp-$Scenario"
New-Item -ItemType Directory -Force -Path $runDir | Out-Null

Write-Host ""
Write-Host "R1.10 deterministic process-recreation verification"
Write-Host "Scenario : $Scenario"
Write-Host "Device   : $serial"
Write-Host "Package  : $Package"
Write-Host ""
Write-Host "PRECONDITION:"
Write-Host "1. Veil Reader is open in an imported EPUB or PDF."
Write-Host "2. Navigate to a distinctive, easy-to-recognize location."
Write-Host "3. Wait at least 2 seconds after the final navigation so locator persistence can settle."
Write-Host ""

$marker = Read-Host "Enter a short locator marker (example: EPUB chapter 7 paragraph start / PDF page 42)"
if ([string]::IsNullOrWhiteSpace($marker)) {
    throw "A locator marker is required so the evidence can be reviewed deterministically."
}

& adb logcat -c
if ($LASTEXITCODE -ne 0) {
    throw "Could not clear logcat."
}

$deviceInfo = [ordered]@{
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

$beforePid = Get-AppPid
if ([string]::IsNullOrWhiteSpace($beforePid)) {
    throw "Veil Reader is not running. Open the reader at the marked location before starting the script."
}
$deviceInfo.beforePid = $beforePid

Save-AdbText -Arguments @("shell", "dumpsys", "activity", "activities") -Path (Join-Path $runDir "01-before-activities.txt")
Save-AdbText -Arguments @("shell", "dumpsys", "activity", "top") -Path (Join-Path $runDir "01-before-top.txt")
Save-AdbText -Arguments @("shell", "dumpsys", "window", "windows") -Path (Join-Path $runDir "01-before-windows.txt")

Invoke-Adb -Arguments @("shell", "input", "keyevent", "KEYCODE_HOME") | Out-Null
Start-Sleep -Seconds 2

$afterHome = (Invoke-Adb -Arguments @("shell", "dumpsys", "activity", "activities") | Out-String)
$afterHome | Set-Content -Path (Join-Path $runDir "02-after-home-activities.txt") -Encoding UTF8

$resumedLine = ($afterHome -split "?
" | Where-Object { $_ -match "mResumedActivity" } | Select-Object -First 1)
if ($resumedLine -and $resumedLine -match [regex]::Escape($Package)) {
    throw "Veil Reader is still the resumed Activity after HOME. Refusing to run an invalid system-kill test."
}

if ($Scenario -eq "system-kill") {
    Invoke-Adb -Arguments @("shell", "am", "kill", $Package) | Out-Null
} else {
    Invoke-Adb -Arguments @("shell", "am", "force-stop", $Package) | Out-Null
}

Wait-ForNoPid -TimeoutSeconds 10

Save-AdbText -Arguments @("shell", "dumpsys", "activity", "activities") -Path (Join-Path $runDir "03-after-termination-activities.txt")
Save-AdbText -Arguments @("shell", "dumpsys", "package", $Package) -Path (Join-Path $runDir "03-after-termination-package.txt")

$launchOutput = Invoke-Adb -Arguments @("shell", "am", "start", "-W", "-n", $Component)
$launchOutput | Set-Content -Path (Join-Path $runDir "04-launch.txt") -Encoding UTF8

$afterPid = Wait-ForPid -TimeoutSeconds 10
$deviceInfo.afterPid = $afterPid
$deviceInfo.pidChanged = ($beforePid -ne $afterPid)

Start-Sleep -Seconds 3

Save-AdbText -Arguments @("shell", "dumpsys", "activity", "activities") -Path (Join-Path $runDir "05-after-relaunch-activities.txt")
Save-AdbText -Arguments @("shell", "dumpsys", "activity", "top") -Path (Join-Path $runDir "05-after-relaunch-top.txt")
Save-AdbText -Arguments @("shell", "dumpsys", "window", "windows") -Path (Join-Path $runDir "05-after-relaunch-windows.txt")
Save-AdbText -Arguments @("logcat", "-d", "-v", "threadtime") -Path (Join-Path $runDir "06-logcat.txt")

$deviceInfo | ConvertTo-Json -Depth 4 | Set-Content -Path (Join-Path $runDir "run.json") -Encoding UTF8

$review = @"
# R1.10 run review

Scenario: $Scenario
Locator marker before termination: $marker
PID before: $beforePid
PID after: $afterPid
PID changed: $($beforePid -ne $afterPid)

## Human verification

Record after relaunch:

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
