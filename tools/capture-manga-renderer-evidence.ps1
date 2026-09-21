param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('baseline','peak','settled','snapshot')]
    [string]$Phase,
    [string]$Serial = '',
    [string]$OutputRoot = 'build/manga-renderer-evidence'
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

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $RepoRoot
$Adb = Find-Adb

$rows = @(
    (& $Adb devices -l) | Select-Object -Skip 1 | Where-Object { $_ -match '^\S+\s+\S+' } | ForEach-Object {
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
    if ($ready.Count -eq 0) { throw 'No authorized online Android device found.' }
    if ($ready.Count -gt 1) { throw ('Multiple online devices found; use -Serial. ' + ($ready.Serial -join ', ')) }
    $selected = $ready[0]
}

$Serial = $selected.Serial
$State = ((& $Adb -s $Serial get-state 2>$null | Out-String).Trim())
if ($State -ne 'device') { throw ('Device changed state to: ' + $State) }

$Dirty = @(git status --porcelain)
if ($LASTEXITCODE -ne 0) { throw 'git status failed.' }
if ($Dirty.Count -gt 0) {
    throw 'Refusing renderer evidence from a dirty worktree; commit or stash changes first.'
}

$AppId = 'com.veilreader.app'
$Commit = (git rev-parse HEAD).Trim()
$Timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$SafeSerial = $Serial -replace '[^A-Za-z0-9_.-]', '_'
$Dir = Join-Path (Join-Path $RepoRoot $OutputRoot) ($Timestamp + '-' + $SafeSerial + '-' + $Phase)
New-Item -ItemType Directory -Force -Path $Dir | Out-Null

function AdbShell([string[]]$ShellArgs) {
    return @(& $Adb -s $Serial shell @ShellArgs 2>&1)
}

$AppPid = ((AdbShell @('pidof',$AppId)) | Out-String).Trim()
if ($Phase -ne 'snapshot' -and -not $AppPid) {
    throw ('Veil Reader is not running; cannot capture ' + $Phase + ' renderer evidence.')
}

$Device = [ordered]@{
    serial = $Serial
    manufacturer = ((AdbShell @('getprop','ro.product.manufacturer')) | Out-String).Trim()
    model = ((AdbShell @('getprop','ro.product.model')) | Out-String).Trim()
    androidRelease = ((AdbShell @('getprop','ro.build.version.release')) | Out-String).Trim()
    api = ((AdbShell @('getprop','ro.build.version.sdk')) | Out-String).Trim()
    securityPatch = ((AdbShell @('getprop','ro.build.version.security_patch')) | Out-String).Trim()
    fingerprint = ((AdbShell @('getprop','ro.build.fingerprint')) | Out-String).Trim()
    screenSize = ((AdbShell @('wm','size')) | Out-String).Trim()
    screenDensity = ((AdbShell @('wm','density')) | Out-String).Trim()
}

$Metadata = [ordered]@{
    phase = $Phase
    capturedAt = (Get-Date).ToString('o')
    commit = $Commit
    appId = $AppId
    pid = $AppPid
    device = $Device
}
$Metadata | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 (Join-Path $Dir 'metadata.json')

AdbShell @('dumpsys','meminfo',$AppId) | Set-Content -Encoding UTF8 (Join-Path $Dir 'meminfo.txt')
AdbShell @('dumpsys','gfxinfo',$AppId) | Set-Content -Encoding UTF8 (Join-Path $Dir 'gfxinfo.txt')
AdbShell @('dumpsys','activity','processes') | Select-String -Pattern $AppId -Context 2,8 | Set-Content -Encoding UTF8 (Join-Path $Dir 'process-state.txt')

$Logcat = @(& $Adb -s $Serial logcat -d -v threadtime 2>&1)
$Logcat | Where-Object {
    $_ -match 'com\.veilreader\.app|OutOfMemory|lowmemorykiller|lmkd|FATAL EXCEPTION|SIGABRT|SIGSEGV'
} | Set-Content -Encoding UTF8 (Join-Path $Dir 'logcat-filtered.txt')

Write-Host ('Captured ' + $Phase + ' renderer evidence: ' + $Dir)
Write-Host ('Device: ' + $Device.manufacturer + ' ' + $Device.model + ' / API ' + $Device.api)
Write-Host ('App PID: ' + $(if ($AppPid) { $AppPid } else { 'not running' }))
exit 0