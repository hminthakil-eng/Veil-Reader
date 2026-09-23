param(
    [string]$EpubFixture = "qa\fixtures\veil-smoke.epub"
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$adb = Join-Path $sdk "platform-tools\adb.exe"
$gradleWrapper = Join-Path $repoRoot "gradlew.bat"
if (-not (Test-Path $adb)) { throw "adb.exe not found under $sdk" }

if (Test-Path $gradleWrapper) {
    $gradle = $gradleWrapper
} else {
    $gradleCommand = Get-Command gradle -ErrorAction SilentlyContinue
    if (-not $gradleCommand) {
        throw "Neither gradlew.bat nor a system Gradle command is available."
    }
    $gradle = $gradleCommand.Source
}

if (-not (Test-Path $EpubFixture)) { throw "EPUB fixture not found: $EpubFixture" }

$devices = @(& $adb devices | Select-String '\s+device$' | ForEach-Object { ($_ -split '\s+')[0] })
if ($devices.Count -ne 1) { throw "Expected exactly one online Android device; found $($devices.Count)." }

$qaDir = Join-Path $repoRoot "app\build\qa\r1-10"
New-Item -ItemType Directory -Force -Path $qaDir | Out-Null

Write-Output "R1.10 gate 1/5: unit + lint + APK + AndroidTest APK"
& $gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
if ($LASTEXITCODE -ne 0) { throw "Gradle verification failed." }

$appApk = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
$testApk = Join-Path $repoRoot "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
if (-not (Test-Path $appApk)) { throw "App APK missing: $appApk" }
if (-not (Test-Path $testApk)) { throw "AndroidTest APK missing: $testApk" }

Write-Output "R1.10 gate 2/5: clean install and seed EPUB fixture"
$installedPath = (& $adb shell pm path com.veilreader.app 2>$null | Out-String).Trim()
if ($installedPath) {
    & $adb shell pm clear com.veilreader.app | Out-Null
}
& $adb install -r $appApk | Out-Null
if ($LASTEXITCODE -ne 0) { throw "App APK installation failed." }
& $adb install -r $testApk | Out-Null
if ($LASTEXITCODE -ne 0) { throw "AndroidTest APK installation failed." }
& $adb push $EpubFixture /sdcard/Download/VeilReaderQa.epub | Out-Null
if ($LASTEXITCODE -ne 0) { throw "EPUB fixture push failed." }

function Invoke-ReaderInstrumentation([string]$ClassName, [string]$OutputName) {
    $result = & $adb shell am instrument -w -r -e class $ClassName com.veilreader.app.test/androidx.test.runner.AndroidJUnitRunner 2>&1
    $result | Set-Content (Join-Path $qaDir $OutputName)
    $joined = $result -join [Environment]::NewLine
    if ($LASTEXITCODE -ne 0 -or $joined -match "FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|shortMsg=Process crashed") {
        throw "Instrumentation failed: $ClassName. See $OutputName"
    }
}

Write-Output "R1.10 gate 3/5: real EPUB Activity recreation"
Invoke-ReaderInstrumentation "com.veilreader.app.ReaderSafImportInstrumentedTest" "epub-recreation.txt"

Write-Output "R1.10 gate 4/5: real PDF zoom/layout/page + Activity recreation"
Invoke-ReaderInstrumentation "com.veilreader.app.ReaderPdfReliabilityInstrumentedTest" "pdf-reliability.txt"

function Get-AppPid {
    $pidOutput = & $adb shell pidof com.veilreader.app 2>$null
    if ($LASTEXITCODE -ne 0) { return "" }
    return (($pidOutput | Out-String).Trim())
}

function Assert-NonZeroReaderProgress([string]$TraceText, [string]$Stage) {
    $readerOpenMatches = [regex]::Matches($TraceText, 'event=reader_open .*initialProgress=([0-9]+(?:\.[0-9]+)?)')
    if ($readerOpenMatches.Count -eq 0) {
        throw "No Reader open trace appeared during $Stage."
    }
    $valueText = $readerOpenMatches[$readerOpenMatches.Count - 1].Groups[1].Value
    $value = [double]::Parse($valueText, [Globalization.CultureInfo]::InvariantCulture)
    if ($value -le 0.0) {
        throw "Reader progress was not non-zero during $Stage: $valueText"
    }
    return $valueText
}

Write-Output "R1.10 gate 5/5: background process death and Reader task restoration"
$beforePid = Get-AppPid
if (-not $beforePid) {
    & $adb logcat -c
    $preflightLaunch = & $adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 -n com.veilreader.app/.MainActivity 2>&1
    $preflightLaunch | Set-Content (Join-Path $qaDir "process-preflight-launch.txt")
    if ($LASTEXITCODE -ne 0 -or ($preflightLaunch -join [Environment]::NewLine) -match "Error:|Exception") {
        throw "Reader preflight launch failed after PDF instrumentation."
    }
    Start-Sleep -Seconds 6
    $beforePid = Get-AppPid
    if (-not $beforePid) { throw "App process did not start for the process-death preflight." }

    $preflightTrace = & $adb logcat -d -s "VeilReaderTrace:D" "*:S"
    $preflightTrace | Set-Content (Join-Path $qaDir "reader-trace-before-process-death.txt")
    $null = Assert-NonZeroReaderProgress -TraceText ($preflightTrace -join [Environment]::NewLine) -Stage "process-death preflight"
}

& $adb shell input keyevent KEYCODE_HOME | Out-Null
Start-Sleep -Seconds 2
& $adb shell am kill com.veilreader.app | Out-Null
Start-Sleep -Seconds 2

$afterKillPid = Get-AppPid
if ($afterKillPid -and $afterKillPid -eq $beforePid) {
    throw "am kill did not recreate the process boundary; PID $beforePid is still alive."
}

& $adb logcat -c
$launch = & $adb shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n com.veilreader.app/.MainActivity 2>&1
$launch | Set-Content (Join-Path $qaDir "process-relaunch.txt")
if ($LASTEXITCODE -ne 0 -or ($launch -join [Environment]::NewLine) -match "Error:|Exception") {
    throw "Reader relaunch failed after process death."
}
Start-Sleep -Seconds 6

$trace = & $adb logcat -d -s "VeilReaderTrace:D" "*:S"
$trace | Set-Content (Join-Path $qaDir "reader-trace-after-process-death.txt")
$traceText = $trace -join [Environment]::NewLine
$progressText = Assert-NonZeroReaderProgress -TraceText $traceText -Stage "process recreation"

$errors = & $adb logcat -d -v brief | Select-String "FATAL EXCEPTION|ANR in com.veilreader.app|Process: com.veilreader.app"
if ($errors) {
    $errors | Set-Content (Join-Path $qaDir "runtime-errors.txt")
    throw "R1.10 process-recreation gate found a crash/ANR."
}

Write-Output "PASS: R1.10 EPUB/PDF recreation and process-death resume gate completed."
Write-Output "Restored durable progress: $progressText"
Write-Output "Evidence: $qaDir"
