param(
    [string]$Serial = '',
    [string]$FixturePath = 'qa/fixtures/veil-smoke.epub',
    [string]$EvidenceRoot = 'build/device-evidence'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Find-Adb {
    $command = Get-Command adb -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    $candidates = @()
    if ($env:ANDROID_HOME) { $candidates += (Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe') }
    if ($env:ANDROID_SDK_ROOT) { $candidates += (Join-Path $env:ANDROID_SDK_ROOT 'platform-tools\adb.exe') }
    if ($env:LOCALAPPDATA) { $candidates += (Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe') }
    foreach ($candidate in $candidates) { if (Test-Path $candidate) { return (Resolve-Path $candidate).Path } }
    throw 'adb was not found.'
}

function Find-Gradle([string]$RepoRoot) {
    $wrapper = Join-Path $RepoRoot 'gradlew.bat'
    if (Test-Path $wrapper) { return (Resolve-Path $wrapper).Path }
    $command = Get-Command gradle -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    throw 'Gradle was not found.'
}

function Device-Prop([string]$Name) {
    return ((& $script:Adb -s $script:SelectedSerial shell getprop $Name 2>$null | Out-String).Trim())
}

function Run-Adb([string[]]$Args) {
    & $script:Adb -s $script:SelectedSerial @Args
    if ($LASTEXITCODE -ne 0) { throw ('adb failed: ' + ($Args -join ' ')) }
}

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $RepoRoot
$script:Adb = Find-Adb
$Gradle = Find-Gradle $RepoRoot
$Fixture = (Resolve-Path (Join-Path $RepoRoot $FixturePath)).Path

$rawDevices = @(& $script:Adb devices -l)
if ($LASTEXITCODE -ne 0) { throw 'Unable to query adb devices.' }
$rows = @(
    $rawDevices | Select-Object -Skip 1 | Where-Object { $_ -match '^\S+\s+\S+' } | ForEach-Object {
        $parts = ($_ -split '\s+')
        [pscustomobject]@{ Serial = $parts[0]; State = $parts[1]; Raw = $_ }
    }
)

if ($Serial) {
    $selected = $rows | Where-Object Serial -eq $Serial | Select-Object -First 1
    if (-not $selected) { throw ('Requested device is not visible: ' + $Serial) }
    if ($selected.State -ne 'device') { throw ('Requested device is not ready: ' + $selected.State) }
} else {
    $ready = @($rows | Where-Object State -eq 'device')
    if ($ready.Count -eq 0) {
        $seen = if ($rows.Count) { $rows.Raw -join ' | ' } else { 'none' }
        throw ('No authorized online Android device found. adb rows: ' + $seen)
    }
    if ($ready.Count -gt 1) { throw ('Multiple online devices found; use -Serial. ' + ($ready.Serial -join ', ')) }
    $selected = $ready[0]
}

$script:SelectedSerial = $selected.Serial
$state = ((& $script:Adb -s $script:SelectedSerial get-state 2>$null | Out-String).Trim())
if ($state -ne 'device') { throw ('Device changed state to: ' + $state) }

$dirty = @(git status --porcelain)
if ($LASTEXITCODE -ne 0) { throw 'git status failed.' }
if ($dirty.Count -gt 0) { throw 'Refusing device evidence from a dirty worktree.' }
$commit = (git rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve git commit.' }

$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$safeSerial = $script:SelectedSerial -replace '[^A-Za-z0-9_.-]', '_'
$evidenceDir = Join-Path (Join-Path $RepoRoot $EvidenceRoot) ($timestamp + '-' + $safeSerial)
New-Item -ItemType Directory -Force -Path $evidenceDir | Out-Null

$device = [ordered]@{
    serial = $script:SelectedSerial
    manufacturer = Device-Prop 'ro.product.manufacturer'
    model = Device-Prop 'ro.product.model'
    device = Device-Prop 'ro.product.device'
    androidRelease = Device-Prop 'ro.build.version.release'
    api = Device-Prop 'ro.build.version.sdk'
    securityPatch = Device-Prop 'ro.build.version.security_patch'
    abi = Device-Prop 'ro.product.cpu.abi'
    buildFingerprint = Device-Prop 'ro.build.fingerprint'
    screenSize = ((& $script:Adb -s $script:SelectedSerial shell wm size 2>$null | Out-String).Trim())
    screenDensity = ((& $script:Adb -s $script:SelectedSerial shell wm density 2>$null | Out-String).Trim())
}
$metadata = [ordered]@{
    status = 'started'
    startedAt = (Get-Date).ToString('o')
    commit = $commit
    fixture = $FixturePath
    fixtureSha256 = (Get-FileHash -Algorithm SHA256 $Fixture).Hash
    device = $device
}
$metadata | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 (Join-Path $evidenceDir 'metadata.json')
$rawDevices | Set-Content -Encoding UTF8 (Join-Path $evidenceDir 'adb-devices.txt')

Write-Host ('Device: ' + $device.manufacturer + ' ' + $device.model + ' / API ' + $device.api)
Write-Host ('Serial: ' + $script:SelectedSerial)
Write-Host ('Commit: ' + $commit)
Write-Host ('Evidence: ' + $evidenceDir)

$documentsUi = ((& $script:Adb -s $script:SelectedSerial shell pm path com.google.android.documentsui 2>$null | Out-String).Trim())
if (-not $documentsUi) { throw 'DocumentsUI is unavailable; SAF smoke cannot run reliably.' }

Run-Adb @('shell','input','keyevent','KEYCODE_WAKEUP')
try { Run-Adb @('shell','wm','dismiss-keyguard') } catch { Write-Warning 'Keyguard could not be dismissed automatically.' }
Run-Adb @('shell','mkdir','-p','/sdcard/Download')
Run-Adb @('push',$Fixture,'/sdcard/Download/VeilReaderQa.epub')

$buildLog = Join-Path $evidenceDir 'gradle-build.log'
& $Gradle --offline :app:assembleDebug :app:assembleDebugAndroidTest --stacktrace 2>&1 | Tee-Object -FilePath $buildLog
$buildExit = $LASTEXITCODE
if ($buildExit -ne 0) {
    $metadata.status = 'build_failed'; $metadata.finishedAt = (Get-Date).ToString('o'); $metadata.buildExitCode = $buildExit
    $metadata | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 (Join-Path $evidenceDir 'metadata.json')
    exit $buildExit
}

$appApk = Join-Path $RepoRoot 'app\build\outputs\apk\debug\app-debug.apk'
$testApk = Join-Path $RepoRoot 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk'
if (-not (Test-Path $appApk)) { throw ('Debug APK missing: ' + $appApk) }
if (-not (Test-Path $testApk)) { throw ('AndroidTest APK missing: ' + $testApk) }
$metadata.appApkSha256 = (Get-FileHash -Algorithm SHA256 $appApk).Hash
$metadata.testApkSha256 = (Get-FileHash -Algorithm SHA256 $testApk).Hash

$install = @()
$install += (& $script:Adb -s $script:SelectedSerial install -r -t $appApk 2>&1); $appInstall = $LASTEXITCODE
$install += (& $script:Adb -s $script:SelectedSerial install -r -t $testApk 2>&1); $testInstall = $LASTEXITCODE
$install | Set-Content -Encoding UTF8 (Join-Path $evidenceDir 'install.log')
if ($appInstall -ne 0 -or $testInstall -ne 0) {
    $metadata.status = 'install_failed'; $metadata.finishedAt = (Get-Date).ToString('o')
    $metadata.appInstallExitCode = $appInstall; $metadata.testInstallExitCode = $testInstall
    $metadata | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 (Join-Path $evidenceDir 'metadata.json')
    throw 'APK install failed; the script will not uninstall user data automatically.'
}

$runners = @(& $script:Adb -s $script:SelectedSerial shell pm list instrumentation)
$runnerLine = $runners | Where-Object { $_ -match '\(target=com\.veilreader\.app\)' } | Select-Object -First 1
if (-not $runnerLine) { throw 'Veil Reader instrumentation runner not found.' }
$runner = (($runnerLine -replace '^instrumentation:', '') -replace '\s+\(target=.*$', '').Trim()

$instrumentArgs = @('shell','am','instrument','-w','-r','-e','class','com.veilreader.app.ReaderSafImportInstrumentedTest',$runner)
$testOutput = & $script:Adb -s $script:SelectedSerial @instrumentArgs 2>&1
$testExit = $LASTEXITCODE
$testOutput | Set-Content -Encoding UTF8 (Join-Path $evidenceDir 'instrumentation.log')
$testText = ($testOutput | Out-String)
$passed = ($testExit -eq 0) -and ($testText -match 'OK \(1 test\)') -and ($testText -notmatch 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed')

$metadata.status = if ($passed) { 'passed' } else { 'failed' }
$metadata.finishedAt = (Get-Date).ToString('o')
$metadata.instrumentationExitCode = $testExit
$metadata.instrumentationRunner = $runner
$metadata | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 (Join-Path $evidenceDir 'metadata.json')

if (-not $passed) { Write-Error ('Reader SAF smoke failed. Evidence: ' + $evidenceDir); exit 1 }
Write-Host ''
Write-Host 'PASS: Reader SAF import -> reader open'
Write-Host ('Evidence saved to: ' + $evidenceDir)
exit 0