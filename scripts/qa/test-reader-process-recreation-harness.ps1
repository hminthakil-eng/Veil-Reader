param(
    [string]$Harness = (Join-Path $PSScriptRoot "verify-reader-process-recreation.ps1")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Assert-Equal($Expected, $Actual, [string]$Message) {
    if ($Expected -ne $Actual) {
        throw "$Message Expected='$Expected' Actual='$Actual'"
    }
}

function Assert-Throws([scriptblock]$Block, [string]$Message) {
    $threw = $false
    try {
        & $Block
    } catch {
        $threw = $true
    }
    if (-not $threw) {
        throw $Message
    }
}

. $Harness -FunctionsOnly

$stopped = @"
  * Task{abc #14 type=standard A=10209:com.veilreader.app U=0 visible=false sz=1}
    * Hist  #0: ActivityRecord{def u0 com.veilreader.app/.MainActivity t14}
      packageName=com.veilreader.app processName=com.veilreader.app
      Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10200000 cmp=com.veilreader.app/.MainActivity }
      rootOfTask=true task=Task{abc #14 type=standard A=10209:com.veilreader.app}
      mActivityComponent=com.veilreader.app/.MainActivity
      mHaveState=true mIcicle=Bundle[mParcelledData.dataSize=6120]
      state=STOPPED delayedResume=false finishing=false
"@

$resumed = $stopped -replace 'state=STOPPED', 'state=RESUMED'
$badTask = $stopped -replace 'cat=\[android.intent.category.LAUNCHER\]', 'cat=[android.intent.category.DEFAULT]'

Assert-Equal "STOPPED" (Get-AppActivityState -ActivitiesText $stopped) "STOPPED parser failed."
Assert-Equal "RESUMED" (Get-AppActivityState -ActivitiesText $resumed) "RESUMED parser failed."
Assert-Equal "" (Parse-SinglePid -Text "") "Empty pidof output must mean process absent."
Assert-Equal "1234" (Parse-SinglePid -Text "1234") "Single PID parsing failed."
Assert-Throws { Parse-SinglePid -Text "1234 5678" } "Multiple PID output must be rejected as ambiguous."
Assert-Throws { Parse-SinglePid -Text "error" } "Malformed PID output must be rejected."

Assert-LauncherRootTaskFromText -ActivitiesText $stopped
Assert-Throws { Assert-LauncherRootTaskFromText -ActivitiesText $badTask } "Non-LAUNCHER root task must be rejected."

$source = Get-Content -Raw $Harness
foreach ($required in @(
    'Wait-ForActivityState -ExpectedState "STOPPED"',
    'Save-BestEffortFailureEvidence',
    'pidof $Package || true',
    'android.intent.category.LAUNCHER',
    'Reopen Veil Reader from the launcher'
)) {
    if ($source -notlike "*$required*") {
        throw "Missing hardened harness contract: $required"
    }
}

if ($source -match 'Start-Sleep -Seconds 3') {
    throw "Fixed 3-second post-launch sleep must not remain in the hardened harness."
}

$tokens = $null
$errors = $null
[System.Management.Automation.Language.Parser]::ParseFile($Harness, [ref]$tokens, [ref]$errors) | Out-Null
if ($errors.Count -gt 0) {
    throw "PowerShell parser errors: $($errors | Out-String)"
}

Write-Output "R1.10 harness self-test: PASS"
