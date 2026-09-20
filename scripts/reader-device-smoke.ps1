param(
    [string]$Apk = "app\build\outputs\apk\debug\app-debug.apk",
    [string]$Publication = "",
    [ValidateSet("application/pdf", "application/epub+zip")]
    [string]$Mime = "application/pdf"
)

$ErrorActionPreference = "Stop"
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$adb = Join-Path $sdk "platform-tools\adb.exe"
if (-not (Test-Path $adb)) { throw "adb.exe not found under $sdk" }
if (-not (Test-Path $Apk)) { throw "APK not found: $Apk" }

$devices = @(& $adb devices | Select-String '\s+device$' | ForEach-Object { ($_ -split '\s+')[0] })
if ($devices.Count -ne 1) { throw "Expected exactly one online Android device; found $($devices.Count)." }

$qaDir = Join-Path (Get-Location) "app\build\qa"
New-Item -ItemType Directory -Force -Path $qaDir | Out-Null

& $adb install -r $Apk
if ($LASTEXITCODE -ne 0) { throw "APK installation failed." }

& $adb logcat -c
& $adb shell am force-stop com.veilreader.app
& $adb shell monkey -p com.veilreader.app -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep -Seconds 3

if ($Publication) {
    if (-not (Test-Path $Publication)) { throw "Publication not found: $Publication" }
    $extension = [IO.Path]::GetExtension($Publication)
    $remoteName = "VeilReaderQa$extension"
    $remote = "/sdcard/Download/$remoteName"
    & $adb push $Publication $remote | Out-Null
    $encodedName = [uri]::EscapeDataString("primary:Download/$remoteName")
    $uri = "content://com.android.externalstorage.documents/document/$encodedName"
    $importOutput = & $adb shell am start -a android.intent.action.VIEW -d $uri -t $Mime --grant-read-uri-permission 2>&1
    $importCode = $LASTEXITCODE
    if ($importCode -ne 0 -or ($importOutput -join [Environment]::NewLine) -match "SecurityException|Permission Denial|Error:") {
        throw "Publication import did not start successfully. The smoke harness must not report PASS when Android SAF rejected the URI."
    }
    Start-Sleep -Seconds 4
}

$shot = Join-Path $qaDir "device-smoke.png"
$cmd = '"' + $adb + '" exec-out screencap -p > "' + $shot + '"'
& cmd.exe /c $cmd

$errors = & $adb logcat -d -v brief | Select-String "FATAL EXCEPTION|ANR in com.veilreader.app|Process: com.veilreader.app"
if ($errors) {
    $errors | Set-Content (Join-Path $qaDir "device-smoke-errors.txt")
    throw "Reader smoke test found a crash/ANR. See app\build\qa\device-smoke-errors.txt"
}

Write-Output "PASS: launch/import smoke test completed and the import intent was accepted without crash/ANR."
Write-Output "Screenshot: $shot"