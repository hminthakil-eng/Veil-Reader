param(
  [string]$Serial = 'emulator-5584',
  [string]$Sdk = 'C:\Users\Microsoft\AppData\Local\Android\Sdk'
)
$ErrorActionPreference = 'Stop'
$adb = Join-Path $Sdk 'platform-tools\adb.exe'
$apk = Join-Path (Split-Path -Parent $PSScriptRoot) 'build\IslandBookshop-QA.apk'
if($Serial -ne 'emulator-5584'){throw 'This script is ONLY authorized for the isolated IslandBookshopQA emulator-5584. Do not touch Reader test devices.'}
if(!(Test-Path $adb) -or !(Test-Path $apk)){throw 'Missing ADB or verified debug APK.'}
$devices=(& $adb devices -l 2>&1)-join "`n"
if($devices -notmatch '(?m)^emulator-5584\s+device\b') {
    throw 'Isolated QA emulator is not ONLINE. Do not install to any other or offline emulator.'
}
$boot=((& $adb -s $Serial shell getprop sys.boot_completed 2>&1)-join '').Trim()
if($boot -ne '1'){throw 'QA emulator not fully booted yet'}
$package='org.eyadstudio.islandbookshop.qa'
$dest=Join-Path (Split-Path -Parent $PSScriptRoot) 'build\qa-device'
New-Item -ItemType Directory -Force -Path $dest|Out-Null
& $adb -s $Serial install -r $apk
if($LASTEXITCODE -ne 0){throw 'APK install rejected by isolated emulator'}
& $adb -s $Serial logcat -c
& $adb -s $Serial shell monkey -p $package 1
if($LASTEXITCODE -ne 0){throw 'Launcher activity failed to start'}
Start-Sleep -Seconds 5
& $adb -s $Serial shell screencap -p '/sdcard/island_bookshop_qa.png'
if($LASTEXITCODE -ne 0){throw 'Screenshot command failed'}
& $adb -s $Serial pull '/sdcard/island_bookshop_qa.png' (Join-Path $dest 'launch.png')
if($LASTEXITCODE -ne 0){throw 'Screenshot pull failed'}
& $adb -s $Serial logcat -d -t 1000 '*:E' > (Join-Path $dest 'logcat-errors.txt')
& $adb -s $Serial shell am force-stop $package
& $adb -s $Serial shell monkey -p $package 1
if($LASTEXITCODE -ne 0){throw 'Relaunch after force-stop failed'}
Write-Output ('ANDROID_QA_DEVICE_BOOT_LAUNCH_RELAUNCH_PASS '+$Serial)
Write-Output ('ANDROID_QA_SCREENSHOT_PATH '+(Join-Path $dest 'launch.png'))
Write-Output 'Manual QA still required: Persian glyphs, safe area, tap/swipe, save/reload choice consistency.'
